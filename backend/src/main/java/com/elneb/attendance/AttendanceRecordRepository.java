package com.elneb.attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, UUID> {
    Optional<AttendanceRecord> findBySessionIdAndTeacherId(UUID sessionId, UUID teacherId);
    List<AttendanceRecord> findAllBySessionIdOrderByCheckInAt(UUID sessionId);
}
