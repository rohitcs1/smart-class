package com.elneb.campus;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface BuildingRepository extends JpaRepository<Building, UUID> {
    List<Building> findAllByCampusIdOrderByName(UUID campusId);
    boolean existsByCampusIdAndNameIgnoreCase(UUID campusId, String name);
}
