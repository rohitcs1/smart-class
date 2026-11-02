package com.elneb.timetable;

import com.elneb.campus.Classroom;
import com.elneb.common.UuidEntitySupport;
import com.elneb.teacher.Teacher;
import jakarta.persistence.*;
import java.time.*;

@Entity
@Table(name = "timetable_entries")
public class TimetableEntry extends UuidEntitySupport {
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "classroom_id")
    private Classroom classroom;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "teacher_id")
    private Teacher teacher;
    @Enumerated(EnumType.STRING) @Column(name = "day_of_week", nullable = false, length = 20)
    private DayOfWeek dayOfWeek;
    @Column(nullable = false, length = 200) private String subject;
    @Column(name = "starts_at", nullable = false) private LocalTime startsAt;
    @Column(name = "ends_at", nullable = false) private LocalTime endsAt;
    @Column(nullable = false) private boolean active = true;
    protected TimetableEntry() {}
    public TimetableEntry(Classroom classroom, Teacher teacher, DayOfWeek dayOfWeek, String subject, LocalTime startsAt, LocalTime endsAt) {
        this.classroom = classroom; this.teacher = teacher; this.dayOfWeek = dayOfWeek; this.subject = subject.trim(); this.startsAt = startsAt; this.endsAt = endsAt;
    }
    public UUIDParts parts() { return new UUIDParts(classroom.getId(), teacher.getId(), dayOfWeek, subject, startsAt, endsAt, active); }
    public record UUIDParts(java.util.UUID classroomId, java.util.UUID teacherId, DayOfWeek dayOfWeek, String subject, LocalTime startsAt, LocalTime endsAt, boolean active) {}
    public Classroom getClassroom() { return classroom; }
    public Teacher getTeacher() { return teacher; }
    public void update(Teacher teacher, DayOfWeek day, String subject, LocalTime startsAt, LocalTime endsAt) {
        this.teacher = teacher; this.dayOfWeek = day; this.subject = subject.trim(); this.startsAt = startsAt; this.endsAt = endsAt;
    }
    public void setActive(boolean active) { this.active = active; }
}
