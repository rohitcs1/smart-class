package com.elneb.campus;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface CampusRepository extends JpaRepository<Campus, UUID> {
    List<Campus> findAllByInstitutionIdOrderByName(UUID institutionId);
    boolean existsByInstitutionIdAndNameIgnoreCase(UUID institutionId, String name);
}
