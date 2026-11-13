package com.yvette.api.staff;

import com.yvette.api.common.ConflictException;
import com.yvette.api.common.NotFoundException;
import com.yvette.api.staff.StaffDtos.CreateStaffRequest;
import com.yvette.api.common.BadRequestException;
import com.yvette.api.staff.StaffDtos.LoginResponse;
import com.yvette.api.staff.StaffDtos.MeDto;
import com.yvette.api.staff.StaffDtos.MfaSetupResponse;
import com.yvette.api.staff.StaffDtos.StaffDto;
import com.yvette.api.staff.StaffDtos.UpdateStaffRequest;
import java.util.List;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.beans.factory.annotation.Value;
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
    private final MfaChallenges mfaChallenges;
    private final boolean mfaRequired;

    /** Used to keep login timing similar whether or not the email exists. */
    private final String dummyHash;

    public StaffService(StaffRepository repository, PasswordEncoder passwordEncoder, TokenService tokens,
                        LoginRateLimiter rateLimiter, MfaChallenges mfaChallenges,
                        @Value("${yvette.security.mfa-required:true}") boolean mfaRequired) {
        this.repository = repository;
        this.mfaChallenges = mfaChallenges;
        this.mfaRequired = mfaRequired;
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
        if (staff.isMfaEnabled()) {
            return LoginResponse.needsCode(mfaChallenges.create(staff.getId()));
        }
        return openSession(staff);
    }

    /** Second step of sign-in: exchanges the ticket from {@link #login} plus an authenticator code for a session. */
    public LoginResponse loginWithCode(String ticket, String code) {
        Long staffId = mfaChallenges.staffIdFor(ticket);
        if (staffId == null) {
            throw new BadCredentialsException("Your sign-in expired. Please enter your password again.");
        }
        StaffUser staff = find(staffId);
        if (!staff.isActive() || !staff.isMfaEnabled() || !acceptCode(staff, code)) {
            mfaChallenges.recordFailure(ticket);
            throw new BadCredentialsException("That code didn't work. Check your authenticator app and try again.");
        }
        mfaChallenges.consume(ticket);
        return openSession(staff);
    }

    private LoginResponse openSession(StaffUser staff) {
        staff.recordLogin();
        TokenService.IssuedToken token = tokens.issue(staff);
        return LoginResponse.session(token.value(), token.expiresAt(), StaffDto.of(staff));
    }

    private boolean acceptCode(StaffUser staff, String code) {
        if (staff.getMfaSecret() == null) return false;
        long step = Totp.matchingStep(staff.getMfaSecret(), code, Totp.currentStep());
        return step >= 0 && staff.acceptMfaStep(step);
    }

    @Transactional(readOnly = true)
    public MeDto me(Long id) {
        return MeDto.of(find(id), mfaRequired);
    }

    public boolean isMfaRequired() {
        return mfaRequired;
    }

    // ---------------------------------------------------------------- two-step sign-in setup

    public MfaSetupResponse startMfaSetup(Long id) {
        StaffUser staff = find(id);
        if (staff.isMfaEnabled()) throw new ConflictException("Two-step sign-in is already on");
        String secret = Totp.newSecret();
        staff.startMfaSetup(secret);
        return new MfaSetupResponse(secret, Totp.otpauthUri("Yvette", staff.getEmail(), secret));
    }

    /** Confirms setup with a first code and returns a fresh session that counts as two-step. */
    public LoginResponse enableMfa(Long id, String code) {
        StaffUser staff = find(id);
        if (staff.isMfaEnabled()) throw new ConflictException("Two-step sign-in is already on");
        if (staff.getMfaSecret() == null) throw new BadRequestException("Start the setup first");
        long step = Totp.matchingStep(staff.getMfaSecret(), code, Totp.currentStep());
        if (step < 0) {
            throw new BadRequestException("That code didn't match. Check the time on your phone and try again.");
        }
        staff.enableMfa(step);
        TokenService.IssuedToken token = tokens.issue(staff);
        return LoginResponse.session(token.value(), token.expiresAt(), StaffDto.of(staff));
    }

    public StaffDto disableOwnMfa(Long id, String code) {
        StaffUser staff = find(id);
        if (!staff.isMfaEnabled()) return StaffDto.of(staff);
        if (mfaRequired) throw new ConflictException("Two-step sign-in is required for all staff in this store");
        if (!acceptCode(staff, code)) throw new BadRequestException("That code didn't work");
        staff.disableMfa();
        return StaffDto.of(staff);
    }

    /** Owner resets someone's two-step sign-in (e.g. lost phone). They set it up again at next sign-in. */
    public StaffDto resetMfa(Long id) {
        StaffUser staff = find(id);
        staff.disableMfa();
        return StaffDto.of(staff);
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
