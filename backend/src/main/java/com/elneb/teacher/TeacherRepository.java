package com.elneb.teacher;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface TeacherRepository extends JpaRepository<Teacher, UUID> {
    List<Teacher> findAllByInstitutionIdOrderByFullName(UUID institutionId);
    boolean existsByInstitutionIdAndEmployeeCodeIgnoreCase(UUID institutionId, String employeeCode);
}
