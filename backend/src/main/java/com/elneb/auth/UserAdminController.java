package com.elneb.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserAdminController {
    private final AuthService service;

    public UserAdminController(AuthService service) {
        this.service = service;
    }

    @PatchMapping("/{userId}/role")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'INSTITUTION_ADMIN')")
    public AuthService.AuthResponse changeRole(@PathVariable UUID userId,
                                                @Valid @RequestBody RoleRequest request) {
        return service.changeRole(userId, request.role());
    }

    public record RoleRequest(@NotNull Role role) {}
}
