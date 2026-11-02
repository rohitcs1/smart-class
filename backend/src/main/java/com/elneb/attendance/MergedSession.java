package com.elneb.attendance;

import com.elneb.campus.Classroom;
import com.elneb.common.UuidEntitySupport;
import com.elneb.teacher.Teacher;
import jakarta.persistence.*;
import java.time.*;
import java.util.*;

@Entity @Table(name="merged_sessions")
public class MergedSession extends UuidEntitySupport {
    @ManyToOne(optional=false, fetch=FetchType.LAZY) @JoinColumn(name="teacher_id") private Teacher teacher;
    @Column(name="session_date", nullable=false) private LocalDate sessionDate;
    @Column(name="starts_at", nullable=false) private LocalDateTime startsAt;
    @Column(name="ends_at", nullable=false) private LocalDateTime endsAt;
    @Column(nullable=false, length=20) private String status;
    @ManyToMany(fetch=FetchType.LAZY)
    @JoinTable(name="merged_session_classrooms", joinColumns=@JoinColumn(name="session_id"), inverseJoinColumns=@JoinColumn(name="classroom_id"))
    private Set<Classroom> classrooms = new HashSet<>();
    protected MergedSession() {}
    public MergedSession(Teacher teacher, LocalDate date, LocalDateTime startsAt, LocalDateTime endsAt, Collection<Classroom> classrooms) {
        this.teacher=teacher; this.sessionDate=date; this.startsAt=startsAt; this.endsAt=endsAt; this.status="SCHEDULED"; this.classrooms.addAll(classrooms);
    }
    public Teacher getTeacher(){return teacher;} public LocalDate getSessionDate(){return sessionDate;} public LocalDateTime getStartsAt(){return startsAt;} public LocalDateTime getEndsAt(){return endsAt;} public String getStatus(){return status;} public Set<Classroom> getClassrooms(){return classrooms;}
    public void start(){status="IN_PROGRESS";} public void complete(){status="COMPLETED";}
}
