package com.yvette.api.staff;

import com.yvette.api.common.ConflictException;
import com.yvette.api.common.NotFoundException;
import com.yvette.api.staff.StaffDtos.CreateStaffRequest;
import com.yvette.api.staff.StaffDtos.LoginResponse;
import com.yvette.api.staff.StaffDtos.StaffDto;
import com.yvette.api.staff.StaffDtos.UpdateStaffRequest;
import java.util.List;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class StaffService {

    private final StaffRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokens;
    private final LoginRateLimiter rateLimiter;

    /** Used to keep login timing similar whether or not the email exists. */
    private final String dummyHash;

    public StaffService(StaffRepository repository, PasswordEncoder passwordEncoder, TokenService tokens,
                        LoginRateLimiter rateLimiter) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.tokens = tokens;
        this.rateLimiter = rateLimiter;
        this.dummyHash = passwordEncoder.encode("timing-equaliser-not-a-real-password");
    }

    public LoginResponse login(String email, String password) {
        String key = StaffUser.normaliseEmail(email);
        if (rateLimiter.isBlocked(key)) {
            throw new LockedException("Too many failed attempts. Try again in 15 minutes.");
        }
        StaffUser staff = repository.findByEmail(key).orElse(null);
        boolean ok = passwordEncoder.matches(password, staff == null ? dummyHash : staff.getPasswordHash());
        if (staff == null || !ok || !staff.isActive()) {
            rateLimiter.recordFailure(key);
            throw new BadCredentialsException("Invalid email or password");
        }
        rateLimiter.reset(key);
        staff.recordLogin();
        TokenService.IssuedToken token = tokens.issue(staff);
        return new LoginResponse(token.value(), token.expiresAt(), StaffDto.of(staff));
    }

    @Transactional(readOnly = true)
    public StaffDto get(Long id) {
        return StaffDto.of(find(id));
    }

    @Transactional(readOnly = true)
    public List<StaffDto> list() {
        return repository.findAllByOrderByFullNameAsc().stream().map(StaffDto::of).toList();
    }

    public StaffDto create(CreateStaffRequest req) {
        String email = StaffUser.normaliseEmail(req.email());
        if (repository.existsByEmail(email)) {
            throw new ConflictException("A staff member with this email already exists");
        }
        StaffUser staff = new StaffUser(email, passwordEncoder.encode(req.password()), req.fullName(), req.role());
        return StaffDto.of(repository.save(staff));
    }

    public StaffDto update(Long id, UpdateStaffRequest req, Long actingStaffId) {
        StaffUser staff = find(id);
        boolean removingOwner = staff.getRole() == StaffRole.OWNER && staff.isActive()
                && (req.role() != StaffRole.OWNER || !req.active());
        if (removingOwner && repository.countByRoleAndActiveTrue(StaffRole.OWNER) <= 1) {
            throw new ConflictException("The store must keep at least one active owner");
        }
        if (id.equals(actingStaffId) && !req.active()) {
            throw new ConflictException("You cannot deactivate your own account");
        }
        staff.changeFullName(req.fullName());
        staff.changeRole(req.role());
        staff.setActive(req.active());
        return StaffDto.of(staff);
    }

    /** Creates the first owner account when the store has no staff yet (see StaffBootstrap). */
    public void createInitialOwnerIfNone(String email, String password, String fullName) {
        if (repository.count() > 0) return;
        repository.save(new StaffUser(email, passwordEncoder.encode(password), fullName, StaffRole.OWNER));
    }

    private StaffUser find(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Staff member not found"));
    }
}
