package com.elneb.camera;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface CameraPermissionRepository extends JpaRepository<CameraPermission,UUID>{
    Optional<CameraPermission> findByClassroomIdAndUserId(UUID classroomId,UUID userId);
}
