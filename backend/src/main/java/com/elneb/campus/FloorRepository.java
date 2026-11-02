package com.elneb.campus;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface FloorRepository extends JpaRepository<Floor, UUID> {
    List<Floor> findAllByBuildingIdOrderByName(UUID buildingId);
    boolean existsByBuildingIdAndNameIgnoreCase(UUID buildingId, String name);
}
