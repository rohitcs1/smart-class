package com.elneb.attendance;

import com.elneb.campus.*;
import com.elneb.teacher.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.*;
import java.util.*;

@RestController @RequestMapping("/api/attendance")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','INSTITUTION_ADMIN','TEACHER','DEVICE_ADMIN')")
public class AttendanceController {
    private final MergedSessionRepository sessions; private final AttendanceRecordRepository records; private final TeacherRepository teachers; private final RfidCardRepository cards; private final ClassroomRepository classrooms;
    public AttendanceController(MergedSessionRepository sessions, AttendanceRecordRepository records, TeacherRepository teachers, RfidCardRepository cards, ClassroomRepository classrooms){this.sessions=sessions;this.records=records;this.teachers=teachers;this.cards=cards;this.classrooms=classrooms;}
    @GetMapping("/sessions")
    public List<SessionResponse> listSessions(@RequestParam LocalDate date){return sessions.findAllBySessionDateOrderByStartsAt(date).stream().map(SessionResponse::from).toList();}
    @PostMapping("/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    public SessionResponse createSession(@Valid @RequestBody SessionRequest r){
        if(!r.endsAt().isAfter(r.startsAt())) throw new IllegalArgumentException("endsAt must be after startsAt");
        Teacher teacher=teachers.findById(r.teacherId()).orElseThrow(()->new IllegalArgumentException("Teacher not found"));
        List<Classroom> rooms=classrooms.findAllById(r.classroomIds()); if(rooms.size()!=r.classroomIds().size()) throw new IllegalArgumentException("One or more classrooms not found");
        return SessionResponse.from(sessions.save(new MergedSession(teacher,r.sessionDate(),r.startsAt(),r.endsAt(),rooms)));
    }
    @GetMapping("/sessions/{sessionId}/records")
    public List<RecordResponse> records(@PathVariable UUID sessionId){return records.findAllBySessionIdOrderByCheckInAt(sessionId).stream().map(RecordResponse::from).toList();}
    @PostMapping("/check-in")
    public RecordResponse checkIn(@Valid @RequestBody CheckInRequest r){
        RfidCard card=cards.findByCardUidIgnoreCase(r.cardUid()).filter(RfidCard::isActive).orElseThrow(()->new IllegalArgumentException("RFID card not found or inactive"));
        Teacher teacher=teachers.findById(r.teacherId()).orElseThrow(()->new IllegalArgumentException("Teacher not found"));
        if(!card.getCardUid().equalsIgnoreCase(r.cardUid())) throw new IllegalArgumentException("RFID card mismatch");
        MergedSession session=sessions.findById(r.sessionId()).orElseThrow(()->new IllegalArgumentException("Session not found"));
        AttendanceRecord existing=records.findBySessionIdAndTeacherId(session.getId(),teacher.getId()).orElse(null);
        if(existing!=null) throw new IllegalArgumentException("Teacher is already checked in for this session");
        session.start(); return RecordResponse.from(records.save(new AttendanceRecord(session,teacher,card.getCardUid())));
    }
    @PostMapping("/{recordId}/check-out")
    public RecordResponse checkOut(@PathVariable UUID recordId){AttendanceRecord record=records.findById(recordId).orElseThrow(()->new IllegalArgumentException("Attendance record not found"));record.checkOut();return RecordResponse.from(records.save(record));}
    public record SessionRequest(@NotNull UUID teacherId,@NotNull @Size(min=1) List<UUID> classroomIds,@NotNull LocalDate sessionDate,@NotNull LocalDateTime startsAt,@NotNull LocalDateTime endsAt){}
    public record CheckInRequest(@NotNull UUID sessionId,@NotNull UUID teacherId,@NotBlank String cardUid){}
    public record SessionResponse(UUID id,UUID teacherId,LocalDate sessionDate,LocalDateTime startsAt,LocalDateTime endsAt,String status,List<UUID> classroomIds){static SessionResponse from(MergedSession s){return new SessionResponse(s.getId(),s.getTeacher().getId(),s.getSessionDate(),s.getStartsAt(),s.getEndsAt(),s.getStatus(),s.getClassrooms().stream().map(Classroom::getId).toList());}}
    public record RecordResponse(UUID id,UUID sessionId,UUID teacherId,String cardUid,LocalDateTime checkInAt,LocalDateTime checkOutAt,String status){static RecordResponse from(AttendanceRecord r){return new RecordResponse(r.getId(),r.getSession().getId(),r.getTeacher().getId(),r.getCardUid(),r.getCheckInAt(),r.getCheckOutAt(),r.getStatus());}}
}
