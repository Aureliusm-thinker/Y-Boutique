package com.yvette.api.staff;

import com.yvette.api.common.CurrentStaff;
import com.yvette.api.staff.StaffDtos.CreateStaffRequest;
import com.yvette.api.staff.StaffDtos.LoginResponse;
import com.yvette.api.staff.StaffDtos.MeDto;
import com.yvette.api.staff.StaffDtos.MfaCodeRequest;
import com.yvette.api.staff.StaffDtos.MfaSetupResponse;
import com.yvette.api.staff.StaffDtos.StaffDto;
import com.yvette.api.staff.StaffDtos.UpdateStaffRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
public class StaffAdminController {

    private final StaffService staff;

    public StaffAdminController(StaffService staff) {
        this.staff = staff;
    }

    @GetMapping("/me")
    @PreAuthorize(Roles.ANY_STAFF)
    public MeDto me(@AuthenticationPrincipal Jwt jwt) {
        return staff.me(CurrentStaff.id(jwt));
    }

    @PostMapping("/me/mfa/setup")
    @PreAuthorize(Roles.ANY_STAFF)
    public MfaSetupResponse startMfaSetup(@AuthenticationPrincipal Jwt jwt) {
        return staff.startMfaSetup(CurrentStaff.id(jwt));
    }

    @PostMapping("/me/mfa/enable")
    @PreAuthorize(Roles.ANY_STAFF)
    public LoginResponse enableMfa(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody MfaCodeRequest request) {
        return staff.enableMfa(CurrentStaff.id(jwt), request.code());
    }

    @PostMapping("/me/mfa/disable")
    @PreAuthorize(Roles.ANY_STAFF)
    public StaffDto disableMfa(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody MfaCodeRequest request) {
        return staff.disableOwnMfa(CurrentStaff.id(jwt), request.code());
    }

    @PostMapping("/staff/{id}/mfa/reset")
    @PreAuthorize(Roles.OWNER_ONLY)
    public StaffDto resetMfa(@PathVariable Long id) {
        return staff.resetMfa(id);
    }

    @GetMapping("/staff")
    @PreAuthorize(Roles.OWNER_ONLY)
    public List<StaffDto> list() {
        return staff.list();
    }

    @PostMapping("/staff")
    @PreAuthorize(Roles.OWNER_ONLY)
    @ResponseStatus(HttpStatus.CREATED)
    public StaffDto create(@Valid @RequestBody CreateStaffRequest request) {
        return staff.create(request);
    }

    @PutMapping("/staff/{id}")
    @PreAuthorize(Roles.OWNER_ONLY)
    public StaffDto update(@PathVariable Long id, @Valid @RequestBody UpdateStaffRequest request,
                           @AuthenticationPrincipal Jwt jwt) {
        return staff.update(id, request, CurrentStaff.id(jwt));
    }
}
