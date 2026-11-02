package com.elneb.teacher;

import com.elneb.institution.Institution;
import com.elneb.institution.InstitutionRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/teachers")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','INSTITUTION_ADMIN')")
public class TeacherController {
    private final TeacherRepository teachers;
    private final RfidCardRepository cards;
    private final InstitutionRepository institutions;
    public TeacherController(TeacherRepository teachers, RfidCardRepository cards, InstitutionRepository institutions) {
        this.teachers = teachers; this.cards = cards; this.institutions = institutions;
    }
    @GetMapping("/institution/{institutionId}")
    public List<TeacherResponse> list(@PathVariable UUID institutionId) {
        return teachers.findAllByInstitutionIdOrderByFullName(institutionId).stream().map(TeacherResponse::from).toList();
    }
    @PostMapping("/institution/{institutionId}")
    @ResponseStatus(HttpStatus.CREATED)
    public TeacherResponse create(@PathVariable UUID institutionId, @Valid @RequestBody TeacherRequest request) {
        if (teachers.existsByInstitutionIdAndEmployeeCodeIgnoreCase(institutionId, request.employeeCode())) throw new IllegalArgumentException("Employee code is already in use");
        Institution institution = institutions.findById(institutionId).orElseThrow(() -> new IllegalArgumentException("Institution not found"));
        return TeacherResponse.from(teachers.save(new Teacher(institution, request.employeeCode(), request.fullName(), request.email(), request.department())));
    }
    @PostMapping("/{teacherId}/rfid")
    public RfidResponse assignRfid(@PathVariable UUID teacherId, @Valid @RequestBody RfidRequest request) {
        if (cards.existsByCardUidIgnoreCase(request.cardUid())) throw new IllegalArgumentException("RFID card is already assigned");
        Teacher teacher = teachers.findById(teacherId).orElseThrow(() -> new IllegalArgumentException("Teacher not found"));
        return RfidResponse.from(cards.save(new RfidCard(teacher, request.cardUid())));
    }
    public record TeacherRequest(@NotBlank String employeeCode, @NotBlank String fullName, @Email String email, String department) {}
    public record RfidRequest(@NotBlank String cardUid) {}
    public record TeacherResponse(UUID id, String employeeCode, String fullName, String email, String department, boolean active) {
        static TeacherResponse from(Teacher t) { return new TeacherResponse(t.getId(), t.getEmployeeCode(), t.getFullName(), t.getEmail(), t.getDepartment(), t.isActive()); }
    }
    public record RfidResponse(UUID id, String cardUid, boolean active) {
        static RfidResponse from(RfidCard c) { return new RfidResponse(c.getId(), c.getCardUid(), c.isActive()); }
    }
}
