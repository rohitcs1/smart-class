package com.elneb.attendance;
import com.elneb.common.UuidEntitySupport;
import com.elneb.teacher.Teacher;
import jakarta.persistence.*;
import java.time.*;
import java.util.UUID;
@Entity @Table(name="attendance_records")
public class AttendanceRecord extends UuidEntitySupport {
    @ManyToOne(optional=false, fetch=FetchType.LAZY) @JoinColumn(name="session_id") private MergedSession session;
    @ManyToOne(optional=false, fetch=FetchType.LAZY) @JoinColumn(name="teacher_id") private Teacher teacher;
    @Column(name="card_uid", nullable=false, length=100) private String cardUid;
    @Column(name="check_in_at", nullable=false) private LocalDateTime checkInAt;
    @Column(name="check_out_at") private LocalDateTime checkOutAt;
    @Column(nullable=false, length=20) private String status;
    protected AttendanceRecord(){}
    public AttendanceRecord(MergedSession session, Teacher teacher, String cardUid){this.session=session;this.teacher=teacher;this.cardUid=cardUid;this.checkInAt=LocalDateTime.now();this.status="PRESENT";}
    public UUID getId(){return id;} public String getCardUid(){return cardUid;} public LocalDateTime getCheckInAt(){return checkInAt;} public LocalDateTime getCheckOutAt(){return checkOutAt;} public String getStatus(){return status;} public MergedSession getSession(){return session;} public Teacher getTeacher(){return teacher;}
    public void checkOut(){if(checkOutAt==null){checkOutAt=LocalDateTime.now();status="COMPLETED";}}
}
