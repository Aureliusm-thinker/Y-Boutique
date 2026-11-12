package com.yvette.api.staff;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * On first start, creates the Owner account from YVETTE_BOOTSTRAP_OWNER_EMAIL / _PASSWORD
 * if no staff exist yet. Does nothing once any staff account exists.
 */
@Component
public class StaffBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StaffBootstrap.class);

    private final StaffService staff;
    private final String email;
    private final String password;

    public StaffBootstrap(StaffService staff,
                          @Value("${yvette.bootstrap.owner-email:}") String email,
                          @Value("${yvette.bootstrap.owner-password:}") String password) {
        this.staff = staff;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email.isBlank() || password.isBlank()) return;
        staff.createInitialOwnerIfNone(email, password, "Store Owner");
        log.info("Bootstrap owner account checked for {}", email);
    }
}
