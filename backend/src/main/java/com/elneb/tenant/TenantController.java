package com.elneb.tenant;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/tenants")
public class TenantController {
    private final TenantRepository tenants;

    public TenantController(TenantRepository tenants) {
        this.tenants = tenants;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public TenantResponse create(@Valid @RequestBody CreateTenantRequest request) {
        if (tenants.existsBySlugIgnoreCase(request.slug())) {
            throw new IllegalArgumentException("Tenant slug is already in use");
        }
        return TenantResponse.from(tenants.save(new Tenant(request.name(), request.slug())));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','INSTITUTION_ADMIN')")
    public TenantResponse get(@PathVariable UUID id) {
        return TenantResponse.from(tenants.findById(id).orElseThrow(() -> new IllegalArgumentException("Tenant not found")));
    }

    public record CreateTenantRequest(@NotBlank String name, @NotBlank String slug) {}
    public record TenantResponse(UUID id, String name, String slug, boolean active) {
        static TenantResponse from(Tenant tenant) {
            return new TenantResponse(tenant.getId(), tenant.getName(), tenant.getSlug(), tenant.isActive());
        }
    }
}
