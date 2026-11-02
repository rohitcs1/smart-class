package com.elneb.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.elneb.security.TenantAccessService;
import com.elneb.tenant.Tenant;
import com.elneb.tenant.TenantRepository;

import java.util.UUID;
import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserAdminController {
    private final AuthService service;
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final TenantRepository tenants;
    private final TenantAccessService access;

    public UserAdminController(AuthService service, UserRepository users, PasswordEncoder passwordEncoder,
                               TenantRepository tenants, TenantAccessService access) {
        this.service = service; this.users = users; this.passwordEncoder = passwordEncoder;
        this.tenants = tenants; this.access = access;
    }

    @PatchMapping("/{userId}/role")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'INSTITUTION_ADMIN')")
    public AuthService.AuthResponse changeRole(@PathVariable UUID userId,
                                                @Valid @RequestBody RoleRequest request,
                                                Authentication authentication) {
        User user = users.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (user.getTenant() == null) throw new IllegalArgumentException("User is not assigned to an institution");
        access.requireTenant(user.getTenant().getId(), authentication);
        if (request.role() == Role.SUPER_ADMIN) throw new IllegalArgumentException("Super admin cannot be assigned here");
        return service.changeRole(userId, request.role());
    }

    @GetMapping("/tenant/{tenantId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'INSTITUTION_ADMIN')")
    public List<UserResponse> list(@PathVariable UUID tenantId, Authentication authentication) {
        access.requireTenant(tenantId, authentication);
        return users.findAllByTenantIdOrderByCreatedAtDesc(tenantId).stream().map(UserResponse::from).toList();
    }

    @PostMapping("/tenant/{tenantId}")
    @PreAuthorize("hasRole('INSTITUTION_ADMIN')")
    public UserResponse create(@PathVariable UUID tenantId, @Valid @RequestBody CreateUserRequest request,
                               Authentication authentication) {
        access.requireTenant(tenantId, authentication);
        if (request.role() == Role.SUPER_ADMIN || request.role() == Role.INSTITUTION_ADMIN) {
            throw new IllegalArgumentException("Only operational staff roles can be created here");
        }
        if (users.existsByEmailIgnoreCase(request.email())) throw new IllegalArgumentException("Email is already registered");
        Tenant tenant = tenants.findById(tenantId).orElseThrow(() -> new IllegalArgumentException("Tenant not found"));
        User user = new User(request.email(), passwordEncoder.encode(request.password()), request.role());
        user.updateProfile(request.displayName(), request.contactNumber());
        user.assignTenant(tenant);
        return UserResponse.from(users.save(user));
    }

    @PatchMapping("/{userId}/active")
    @PreAuthorize("hasRole('INSTITUTION_ADMIN')")
    public UserResponse setActive(@PathVariable UUID userId, @RequestParam boolean active,
                                  Authentication authentication) {
        User user = users.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (user.getTenant() == null) throw new IllegalArgumentException("User is not assigned to an institution");
        access.requireTenant(user.getTenant().getId(), authentication);
        user.setActive(active);
        return UserResponse.from(users.save(user));
    }

    public record RoleRequest(@NotNull Role role) {}
    public record CreateUserRequest(@NotBlank @jakarta.validation.constraints.Email String email,
                                    @NotBlank @jakarta.validation.constraints.Size(min = 8, max = 72) String password,
                                    @NotNull Role role, @NotBlank String displayName, String contactNumber) {}
    public record UserResponse(UUID id, String email, String displayName, String contactNumber, Role role, boolean active) {
        static UserResponse from(User user) {
            return new UserResponse(user.getId(), user.getEmail(), user.getDisplayName(), user.getContactNumber(),
                    user.getRole(), user.isActive());
        }
    }
}
