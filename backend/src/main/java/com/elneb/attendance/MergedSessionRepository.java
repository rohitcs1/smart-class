package com.elneb.attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.*;
public interface MergedSessionRepository extends JpaRepository<MergedSession, UUID> {
    List<MergedSession> findAllBySessionDateOrderByStartsAt(LocalDate date);
    List<MergedSession> findAllByStatusNot(String status);
}
