package com.elneb.institution;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface InstitutionRepository extends JpaRepository<Institution, UUID> {
    List<Institution> findAllByTenantIdOrderByName(UUID tenantId);
    boolean existsByTenantIdAndCodeIgnoreCase(UUID tenantId, String code);
}
