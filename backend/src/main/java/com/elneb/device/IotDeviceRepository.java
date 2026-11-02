package com.elneb.device;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface IotDeviceRepository extends JpaRepository<IotDevice, UUID> {
    boolean existsByDeviceCodeIgnoreCase(String deviceCode);
    Optional<IotDevice> findByDeviceCodeIgnoreCase(String deviceCode);
    List<IotDevice> findAllByClassroomIdOrderByDeviceCode(UUID classroomId);
}
