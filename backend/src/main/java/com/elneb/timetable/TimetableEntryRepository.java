package com.elneb.timetable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.DayOfWeek;
import java.util.*;
public interface TimetableEntryRepository extends JpaRepository<TimetableEntry, UUID> {
    List<TimetableEntry> findAllByClassroomIdAndDayOfWeekOrderByStartsAt(UUID classroomId, DayOfWeek dayOfWeek);
}
