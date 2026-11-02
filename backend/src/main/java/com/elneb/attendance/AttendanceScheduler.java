package com.elneb.attendance;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Component
public class AttendanceScheduler {
    private final MergedSessionRepository sessions;
    private final AttendanceRecordRepository records;

    public AttendanceScheduler(MergedSessionRepository sessions, AttendanceRecordRepository records) {
        this.sessions = sessions;
        this.records = records;
    }

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void closeExpiredSessions() {
        LocalDateTime now = LocalDateTime.now();
        sessions.findAllByStatusNot("COMPLETED").stream()
                .filter(session -> session.getEndsAt().isBefore(now))
                .forEach(session -> {
                    records.findAllBySessionIdOrderByCheckInAt(session.getId()).forEach(AttendanceRecord::checkOut);
                    session.complete();
                });
    }
}
