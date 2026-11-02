package com.elneb.teacher;

import com.elneb.institution.Institution;
import com.elneb.institution.InstitutionRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import org.springframework.security.core.Authentication;
import com.elneb.security.TenantAccessService;

@RestController
@RequestMapping("/api/teachers")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','INSTITUTION_ADMIN')")
public class TeacherController {
    private final TeacherRepository teachers;
    private final RfidCardRepository cards;
    private final InstitutionRepository institutions;
    private final TenantAccessService access;
    public TeacherController(TeacherRepository teachers, RfidCardRepository cards, InstitutionRepository institutions, TenantAccessService access) {
        this.teachers = teachers; this.cards = cards; this.institutions = institutions; this.access = access;
    }
    @GetMapping("/institution/{institutionId}")
    public List<TeacherResponse> list(@PathVariable UUID institutionId, Authentication authentication) {
        access.requireInstitution(institutionId, authentication);
        return teachers.findAllByInstitutionIdOrderByFullName(institutionId).stream().map(TeacherResponse::from).toList();
    }
    @PostMapping("/institution/{institutionId}")
    @ResponseStatus(HttpStatus.CREATED)
    public TeacherResponse create(@PathVariable UUID institutionId, @Valid @RequestBody TeacherRequest request, Authentication authentication) {
        access.requireInstitution(institutionId, authentication);
        if (teachers.existsByInstitutionIdAndEmployeeCodeIgnoreCase(institutionId, request.employeeCode())) throw new IllegalArgumentException("Employee code is already in use");
        Institution institution = institutions.findById(institutionId).orElseThrow(() -> new IllegalArgumentException("Institution not found"));
        return TeacherResponse.from(teachers.save(new Teacher(institution, request.employeeCode(), request.fullName(), request.email(), request.department())));
    }

    @PutMapping("/{teacherId}")
    public TeacherResponse update(@PathVariable UUID teacherId, @Valid @RequestBody TeacherRequest request, Authentication authentication) {
        Teacher teacher = teachers.findById(teacherId).orElseThrow(() -> new IllegalArgumentException("Teacher not found"));
        access.requireTeacher(teacher, authentication); teacher.update(request.employeeCode(), request.fullName(), request.email(), request.department());
        return TeacherResponse.from(teachers.save(teacher));
    }

    @DeleteMapping("/{teacherId}")
    public void delete(@PathVariable UUID teacherId, Authentication authentication) {
        Teacher teacher = teachers.findById(teacherId).orElseThrow(() -> new IllegalArgumentException("Teacher not found"));
        access.requireTeacher(teacher, authentication); teacher.setActive(false); cards.findByTeacherId(teacherId).ifPresent(card -> card.setActive(false)); teachers.save(teacher);
    }
    @PostMapping("/{teacherId}/rfid")
    public RfidResponse assignRfid(@PathVariable UUID teacherId, @Valid @RequestBody RfidRequest request, Authentication authentication) {
        if (cards.existsByCardUidIgnoreCase(request.cardUid())) throw new IllegalArgumentException("RFID card is already assigned");
        Teacher teacher = teachers.findById(teacherId).orElseThrow(() -> new IllegalArgumentException("Teacher not found"));
        access.requireTeacher(teacher, authentication);
        return RfidResponse.from(cards.save(new RfidCard(teacher, request.cardUid())));
    }

    @PatchMapping("/{teacherId}/rfid/active")
    public RfidResponse setRfidActive(@PathVariable UUID teacherId, @RequestParam boolean active, Authentication authentication) {
        Teacher teacher = teachers.findById(teacherId).orElseThrow(() -> new IllegalArgumentException("Teacher not found"));
        access.requireTeacher(teacher, authentication);
        RfidCard card = cards.findByTeacherId(teacherId).orElseThrow(() -> new IllegalArgumentException("RFID card not assigned"));
        card.setActive(active); return RfidResponse.from(cards.save(card));
    }

    @DeleteMapping("/{teacherId}/rfid")
    public void unassignRfid(@PathVariable UUID teacherId, Authentication authentication) {
        Teacher teacher = teachers.findById(teacherId).orElseThrow(() -> new IllegalArgumentException("Teacher not found"));
        access.requireTeacher(teacher, authentication);
        cards.findByTeacherId(teacherId).ifPresent(card -> { card.setActive(false); cards.save(card); });
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
