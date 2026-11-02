package com.elneb.report;

import com.elneb.attendance.*;
import com.elneb.campus.Classroom;
import com.elneb.campus.ClassroomRepository;
import com.elneb.device.*;
import com.elneb.teacher.TeacherRepository;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;

@RestController
@RequestMapping("/api/reports")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','INSTITUTION_ADMIN')")
@Transactional(readOnly = true)
public class ReportController {
    private final AttendanceRecordRepository records;
    private final MergedSessionRepository sessions;
    private final ClassroomRepository classrooms;
    private final TeacherRepository teachers;
    private final IotDeviceRepository devices;

    public ReportController(AttendanceRecordRepository records, MergedSessionRepository sessions,
                            ClassroomRepository classrooms, TeacherRepository teachers,
                            IotDeviceRepository devices) {
        this.records = records; this.sessions = sessions; this.classrooms = classrooms;
        this.teachers = teachers; this.devices = devices;
    }

    @GetMapping("/summary")
    public Summary summary(@RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to) {
        DateRange range = DateRange.of(from, to);
        List<MergedSession> sessionRows = sessions.findAll().stream().filter(row -> range.includes(row.getSessionDate())).toList();
        List<AttendanceRecord> attendanceRows = records.findAll().stream().filter(row -> range.includes(row.getSession().getSessionDate())).toList();
        long online = devices.findAll().stream().filter(device -> device.getStatus() == DeviceStatus.ONLINE).count();
        return new Summary(sessionRows.size(), attendanceRows.size(), classrooms.count(), teachers.count(), devices.count(), online,
                attendanceRows.stream().filter(row -> row.getStatus().equals("PRESENT")).count(),
                attendanceRows.stream().filter(row -> row.getStatus().equals("COMPLETED")).count());
    }

    @GetMapping("/attendance")
    public List<AttendanceRow> attendance(@RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to) {
        DateRange range = DateRange.of(from, to);
        return records.findAll().stream().filter(row -> range.includes(row.getSession().getSessionDate()))
                .sorted(Comparator.comparing(AttendanceRecord::getCheckInAt).reversed())
                .map(row -> new AttendanceRow(row.getSession().getSessionDate(), row.getTeacher().getFullName(),
                        row.getTeacher().getEmployeeCode(), row.getCardUid(), row.getCheckInAt(), row.getCheckOutAt(),
                        row.getStatus(), row.getSession().getClassrooms().stream().map(Classroom::getName).sorted().toList()))
                .toList();
    }

    @GetMapping("/classroom-utilization")
    public List<ClassroomUtilization> classroomUtilization(@RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to) {
        DateRange range = DateRange.of(from, to);
        Map<UUID, Long> counts = new HashMap<>();
        sessions.findAll().stream().filter(row -> range.includes(row.getSessionDate()))
                .forEach(row -> row.getClassrooms().forEach(room -> counts.merge(room.getId(), 1L, Long::sum)));
        return classrooms.findAll().stream().map(room -> new ClassroomUtilization(room.getId(), room.getName(),
                room.getCapacity(), counts.getOrDefault(room.getId(), 0L))).toList();
    }

    @GetMapping("/teacher-activity")
    public List<TeacherActivity> teacherActivity(@RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to) {
        DateRange range = DateRange.of(from, to);
        Map<UUID, Long> counts = new HashMap<>();
        records.findAll().stream().filter(row -> range.includes(row.getSession().getSessionDate()))
                .forEach(row -> counts.merge(row.getTeacher().getId(), 1L, Long::sum));
        return teachers.findAll().stream().map(teacher -> new TeacherActivity(teacher.getId(), teacher.getEmployeeCode(),
                teacher.getFullName(), teacher.getDepartment(), counts.getOrDefault(teacher.getId(), 0L))).toList();
    }

    @GetMapping("/device-uptime")
    public List<DeviceUptime> deviceUptime() {
        return devices.findAll().stream().map(device -> new DeviceUptime(device.getId(), device.getDeviceCode(),
                device.getClassroom().getName(), device.getStatus().name(), device.getLastSeenAt())).toList();
    }

    @GetMapping("/export")
    public ResponseEntity<ByteArrayResource> export(@RequestParam(defaultValue = "attendance") String type,
                                                    @RequestParam(defaultValue = "csv") String format,
                                                    @RequestParam(required = false) LocalDate from,
                                                    @RequestParam(required = false) LocalDate to) {
        String csv = csv(type, from, to);
        boolean pdf = format.equalsIgnoreCase("pdf");
        byte[] content = pdf ? SimplePdf.document(type, csv) : csv.getBytes(StandardCharsets.UTF_8);
        String extension = pdf ? "pdf" : "csv";
        MediaType mediaType = pdf ? MediaType.APPLICATION_PDF : MediaType.parseMediaType("text/csv");
        return ResponseEntity.ok().contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=elneb-" + type + "." + extension)
                .body(new ByteArrayResource(content));
    }

    private String csv(String type, LocalDate from, LocalDate to) {
        if (type.equalsIgnoreCase("classroom-utilization")) {
            return rows(List.of("Classroom", "Capacity", "Sessions"), classroomUtilization(from, to).stream()
                    .map(row -> List.of(row.name(), row.capacity(), row.sessions())).toList());
        }
        if (type.equalsIgnoreCase("teacher-activity")) {
            return rows(List.of("Employee Code", "Teacher", "Department", "Attendance Events"), teacherActivity(from, to).stream()
                    .map(row -> List.of(row.employeeCode(), row.fullName(), row.department(), row.attendanceEvents())).toList());
        }
        if (type.equalsIgnoreCase("device-uptime")) {
            return rows(List.of("Device Code", "Classroom", "Status", "Last Seen"), deviceUptime().stream()
                    .map(row -> List.of(row.deviceCode(), row.classroomName(), row.status(), row.lastSeenAt())).toList());
        }
        return rows(List.of("Date", "Teacher", "Employee Code", "RFID", "Check In", "Check Out", "Status", "Classrooms"),
                attendance(from, to).stream().map(row -> List.of(row.date(), row.teacher(), row.employeeCode(), row.cardUid(),
                        row.checkIn(), row.checkOut(), row.status(), String.join(" | ", row.classrooms()))).toList());
    }

    private static String rows(List<String> header, List<? extends List<?>> values) {
        StringBuilder output = new StringBuilder(String.join(",", header)).append('\n');
        for (List<?> row : values) {
            output.append(row.stream().map(ReportController::csvValue).reduce((a, b) -> a + "," + b).orElse("")).append('\n');
        }
        return output.toString();
    }

    private static String csvValue(Object value) {
        return "\"" + Objects.toString(value, "").replace("\"", "\"\"") + "\"";
    }

    private record DateRange(LocalDate from, LocalDate to) {
        static DateRange of(LocalDate from, LocalDate to) {
            LocalDate end = to == null ? LocalDate.now() : to;
            LocalDate start = from == null ? end.minusDays(30) : from;
            if (end.isBefore(start)) throw new IllegalArgumentException("to must not be before from");
            return new DateRange(start, end);
        }
        boolean includes(LocalDate date) { return !date.isBefore(from) && !date.isAfter(to); }
    }

    public record Summary(long sessions, long attendanceRecords, long classrooms, long teachers, long devices,
                          long onlineDevices, long present, long completed) {}
    public record AttendanceRow(LocalDate date, String teacher, String employeeCode, String cardUid,
                                LocalDateTime checkIn, LocalDateTime checkOut, String status, List<String> classrooms) {}
    public record ClassroomUtilization(UUID id, String name, int capacity, long sessions) {}
    public record TeacherActivity(UUID id, String employeeCode, String fullName, String department, long attendanceEvents) {}
    public record DeviceUptime(UUID id, String deviceCode, String classroomName, String status, Instant lastSeenAt) {}

    private static final class SimplePdf {
        static byte[] document(String title, String content) {
            String text = (title + "\n\n" + content).replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
            String body = "BT /F1 9 Tf 40 780 Td (" + text.replace("\n", ") Tj 0 -12 Td (") + ") Tj ET";
            String[] objects = {"<< /Type /Catalog /Pages 2 0 R >>", "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
                    "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>",
                    "<< /Type /Font /Subtype /Type1 /BaseFont /Courier >>", "<< /Length " + body.length() + " >>\nstream\n" + body + "\nendstream"};
            StringBuilder pdf = new StringBuilder("%PDF-1.4\n");
            List<Integer> offsets = new ArrayList<>();
            for (int i = 0; i < objects.length; i++) {
                offsets.add(pdf.length());
                pdf.append(i + 1).append(" 0 obj\n").append(objects[i]).append("\nendobj\n");
            }
            int xref = pdf.length();
            pdf.append("xref\n0 ").append(objects.length + 1).append("\n0000000000 65535 f \n");
            offsets.forEach(offset -> pdf.append(String.format("%010d 00000 n \n", offset)));
            pdf.append("trailer\n<< /Size ").append(objects.length + 1).append(" /Root 1 0 R >>\nstartxref\n")
                    .append(xref).append("\n%%EOF");
            return pdf.toString().getBytes(StandardCharsets.ISO_8859_1);
        }
    }
}
