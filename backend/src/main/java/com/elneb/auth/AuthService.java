package com.elneb.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
import com.elneb.tenant.Tenant;
import com.elneb.tenant.TenantRepository;
import com.elneb.subscription.SubscriptionLifecycleService;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TenantRepository tenants;
    private final SubscriptionLifecycleService subscriptions;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService,
                       TenantRepository tenants, SubscriptionLifecycleService subscriptions) {
        this.users = users; this.passwordEncoder = passwordEncoder; this.jwtService = jwtService;
        this.tenants = tenants; this.subscriptions = subscriptions;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (users.existsByEmailIgnoreCase(request.email())) throw new IllegalArgumentException("Email is already registered");
        String baseSlug = request.email().substring(0, request.email().indexOf('@')).replaceAll("[^a-zA-Z0-9-]", "-").toLowerCase();
        String slug = baseSlug + "-" + UUID.randomUUID().toString().substring(0, 8);
        Tenant tenant = tenants.save(new Tenant(request.email().substring(0, request.email().indexOf('@')) + " workspace", slug));
        User user = new User(request.email(), passwordEncoder.encode(request.password()), Role.INSTITUTION_ADMIN);
        user.assignTenant(tenant);
        user = users.save(user);
        subscriptions.createTrial(tenant);
        return new AuthResponse(jwtService.create(user), user.getEmail(), user.getRole().name());
    }

    public AuthResponse login(LoginRequest request) {
        User user = users.findByEmailIgnoreCase(request.email())
                .filter(User::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }
        return new AuthResponse(jwtService.create(user), user.getEmail(), user.getRole().name());
    }

    @Transactional(readOnly = true)
    public AuthResponse currentUser(UUID userId) {
        User user = users.findById(userId)
                .filter(User::isActive)
                .orElseThrow(() -> new IllegalArgumentException("User account is unavailable"));
        return new AuthResponse(jwtService.create(user), user.getEmail(), user.getRole().name());
    }

    @Transactional
    public AuthResponse changeRole(UUID userId, Role role) {
        User user = users.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User account was not found"));
        if (role == Role.SUPER_ADMIN) {
            throw new IllegalArgumentException("Super admin cannot be assigned through this endpoint");
        }
        user.changeRole(role);
        return new AuthResponse(jwtService.create(user), user.getEmail(), user.getRole().name());
    }

    public record RegisterRequest(@NotBlank @Email String email, @NotBlank @Size(min = 8, max = 72) String password) {}
    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}
    public record AuthResponse(String token, String email, String role) {}
}
