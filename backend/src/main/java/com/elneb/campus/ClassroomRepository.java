package com.elneb.campus;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface ClassroomRepository extends JpaRepository<Classroom, UUID> {
    List<Classroom> findAllByFloorIdOrderByName(UUID floorId);
    boolean existsByFloorIdAndNameIgnoreCase(UUID floorId, String name);
}
