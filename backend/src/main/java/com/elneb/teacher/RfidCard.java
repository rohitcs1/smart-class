package com.elneb.teacher;

import com.elneb.common.UuidEntitySupport;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "rfid_cards")
public class RfidCard extends UuidEntitySupport {
    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false, unique = true)
    private Teacher teacher;
    @Column(name = "card_uid", nullable = false, unique = true, length = 100)
    private String cardUid;
    @Column(nullable = false)
    private boolean active = true;
    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt = Instant.now();
    protected RfidCard() {}
    public RfidCard(Teacher teacher, String cardUid) { this.teacher = teacher; this.cardUid = cardUid.trim().toUpperCase(); }
    public Teacher getTeacher() { return teacher; }
    public String getCardUid() { return cardUid; }
    public boolean isActive() { return active; }
}
