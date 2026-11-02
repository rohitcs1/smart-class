package com.elneb.timetable;

import com.elneb.campus.*;
import com.elneb.teacher.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.*;
import java.util.*;

@RestController @RequestMapping("/api/timetable")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','INSTITUTION_ADMIN')")
public class TimetableController {
    private final TimetableEntryRepository entries; private final ClassroomRepository classrooms; private final TeacherRepository teachers;
    public TimetableController(TimetableEntryRepository entries, ClassroomRepository classrooms, TeacherRepository teachers) { this.entries=entries; this.classrooms=classrooms; this.teachers=teachers; }
    @GetMapping("/classrooms/{classroomId}")
    public List<TimetableResponse> list(@PathVariable UUID classroomId, @RequestParam DayOfWeek day) {
        return entries.findAllByClassroomIdAndDayOfWeekOrderByStartsAt(classroomId, day).stream().map(TimetableResponse::from).toList();
    }
    @PostMapping("/classrooms/{classroomId}")
    @ResponseStatus(HttpStatus.CREATED)
    public TimetableResponse create(@PathVariable UUID classroomId, @Valid @RequestBody TimetableRequest r) {
        if (!r.endsAt().isAfter(r.startsAt())) throw new IllegalArgumentException("endsAt must be after startsAt");
        Classroom room=classrooms.findById(classroomId).orElseThrow(() -> new IllegalArgumentException("Classroom not found"));
        Teacher teacher=teachers.findById(r.teacherId()).orElseThrow(() -> new IllegalArgumentException("Teacher not found"));
        return TimetableResponse.from(entries.save(new TimetableEntry(room, teacher, r.dayOfWeek(), r.subject(), r.startsAt(), r.endsAt())));
    }
    public record TimetableRequest(@NotNull UUID teacherId, @NotNull DayOfWeek dayOfWeek, @NotBlank String subject, @NotNull LocalTime startsAt, @NotNull LocalTime endsAt) {}
    public record TimetableResponse(UUID id, UUID classroomId, UUID teacherId, DayOfWeek dayOfWeek, String subject, LocalTime startsAt, LocalTime endsAt, boolean active) {
        static TimetableResponse from(TimetableEntry x) { var p=x.parts(); return new TimetableResponse(x.getId(),p.classroomId(),p.teacherId(),p.dayOfWeek(),p.subject(),p.startsAt(),p.endsAt(),p.active()); }
    }
}
