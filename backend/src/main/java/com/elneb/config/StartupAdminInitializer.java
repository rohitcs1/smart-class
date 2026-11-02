package com.elneb.config;

import com.elneb.auth.Role;
import com.elneb.auth.User;
import com.elneb.auth.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class StartupAdminInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(StartupAdminInitializer.class);

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;

    public StartupAdminInitializer(
            UserRepository users,
            PasswordEncoder passwordEncoder,
            @Value("${elneb.super-admin.email:}") String email,
            @Value("${elneb.super-admin.password:}") String password) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.email = email == null ? "" : email.trim();
        this.password = password == null ? "" : password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (email.isBlank() && password.isBlank()) {
            log.info("Super-admin bootstrap skipped; configure SUPER_ADMIN_EMAIL and SUPER_ADMIN_PASSWORD to create one.");
            return;
        }
        if (email.isBlank() || password.isBlank()) {
            throw new IllegalStateException("SUPER_ADMIN_EMAIL and SUPER_ADMIN_PASSWORD must be configured together");
        }
        if (password.length() < 12) {
            throw new IllegalStateException("SUPER_ADMIN_PASSWORD must contain at least 12 characters");
        }
        if (!email.contains("@")) {
            throw new IllegalStateException("SUPER_ADMIN_EMAIL must be a valid email address");
        }
        if (users.existsByEmailIgnoreCase(email)) {
            log.info("Configured super-admin account already exists; no password was changed.");
            return;
        }
        users.save(new User(email, passwordEncoder.encode(password), Role.SUPER_ADMIN));
        log.info("Initial super-admin account created for {}", email);
    }
}
