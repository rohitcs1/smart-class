package com.elneb.institution;

import com.elneb.tenant.Tenant;
import com.elneb.tenant.TenantRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import com.elneb.security.TenantAccessService;

@RestController
@RequestMapping("/api/institutions")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','INSTITUTION_ADMIN')")
public class InstitutionController {
    private final InstitutionRepository institutions;
    private final TenantRepository tenants;
    private final TenantAccessService access;

    public InstitutionController(InstitutionRepository institutions, TenantRepository tenants, TenantAccessService access) {
        this.institutions = institutions; this.tenants = tenants; this.access = access;
    }

    @GetMapping("/tenant/{tenantId}")
    public List<InstitutionResponse> list(@PathVariable UUID tenantId, Authentication authentication) {
        access.requireTenant(tenantId, authentication);
        return institutions.findAllByTenantIdOrderByName(tenantId).stream().map(InstitutionResponse::from).toList();
    }

    @PostMapping("/tenant/{tenantId}")
    @ResponseStatus(HttpStatus.CREATED)
    public InstitutionResponse create(@PathVariable UUID tenantId, @Valid @RequestBody CreateInstitutionRequest request, Authentication authentication) {
        access.requireTenant(tenantId, authentication);
        Tenant tenant = tenants.findById(tenantId).orElseThrow(() -> new IllegalArgumentException("Tenant not found"));
        if (institutions.existsByTenantIdAndCodeIgnoreCase(tenantId, request.code())) {
            throw new IllegalArgumentException("Institution code is already in use");
        }
        return InstitutionResponse.from(institutions.save(new Institution(tenant, request.name(), request.code(), request.address())));
    }

    public record CreateInstitutionRequest(@NotBlank String name, @NotBlank String code, String address) {}
    public record InstitutionResponse(UUID id, String name, String code, String address, boolean active) {
        static InstitutionResponse from(Institution item) {
            return new InstitutionResponse(item.getId(), item.getName(), item.getCode(), item.getAddress(), item.isActive());
        }
    }
}
