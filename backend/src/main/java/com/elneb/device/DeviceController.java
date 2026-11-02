package com.elneb.device;

import com.elneb.attendance.*;
import com.elneb.campus.*;
import com.elneb.teacher.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;

@RestController
public class DeviceController {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final IotDeviceRepository devices;
    private final ClassroomRepository classrooms;
    private final PasswordEncoder passwords;
    private final RfidCardRepository cards;
    private final TeacherRepository teachers;
    private final MergedSessionRepository sessions;
    private final AttendanceRecordRepository records;

    public DeviceController(IotDeviceRepository devices, ClassroomRepository classrooms, PasswordEncoder passwords,
                            RfidCardRepository cards, TeacherRepository teachers, MergedSessionRepository sessions,
                            AttendanceRecordRepository records) {
        this.devices = devices; this.classrooms = classrooms; this.passwords = passwords; this.cards = cards;
        this.teachers = teachers; this.sessions = sessions; this.records = records;
    }

    @GetMapping("/api/devices/classroom/{classroomId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','INSTITUTION_ADMIN','DEVICE_ADMIN')")
    public List<DeviceResponse> list(@PathVariable UUID classroomId) {
        return devices.findAllByClassroomIdOrderByDeviceCode(classroomId).stream().map(DeviceResponse::from).toList();
    }

    @PostMapping("/api/devices")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','INSTITUTION_ADMIN')")
    public ProvisionResponse provision(@Valid @RequestBody ProvisionRequest request) {
        if (devices.existsByDeviceCodeIgnoreCase(request.deviceCode())) throw new IllegalArgumentException("Device code is already in use");
        Classroom classroom = classrooms.findById(request.classroomId()).orElseThrow(() -> new IllegalArgumentException("Classroom not found"));
        String setupToken = randomToken();
        IotDevice device = devices.save(new IotDevice(classroom, request.deviceCode().trim().toUpperCase(), passwords.encode(setupToken)));
        return ProvisionResponse.from(device, setupToken);
    }

    @GetMapping(value = "/api/devices/{deviceId}/firmware", produces = "text/plain")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','INSTITUTION_ADMIN')")
    public ResponseEntity<StreamingResponseBody> firmware(@PathVariable UUID deviceId, @RequestParam String setupToken,
                                                            @RequestParam String wifiSsid, @RequestParam String wifiPassword, @RequestParam String apiBaseUrl) {
        IotDevice device = devices.findById(deviceId).orElseThrow(() -> new IllegalArgumentException("Device not found"));
        authenticate(device.getDeviceCode(), setupToken);
        String source = firmwareTemplate()
                .replace("REPLACE_WITH_WIFI_SSID", escape(wifiSsid))
                .replace("REPLACE_WITH_WIFI_PASSWORD", escape(wifiPassword))
                .replace("REPLACE_WITH_DEVICE_ID", escape(device.getDeviceCode()))
                .replace("REPLACE_WITH_SETUP_TOKEN", setupToken)
                .replace("http://localhost:8080/api", escape(apiBaseUrl));
        StreamingResponseBody body = output -> output.write(source.getBytes(StandardCharsets.UTF_8));
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=ELNEB_" + device.getDeviceCode() + ".ino").body(body);
    }

    @PostMapping("/api/device/heartbeat")
    public DeviceResponse heartbeat(@Valid @RequestBody DeviceRequest request) {
        IotDevice device = authenticate(request.deviceCode(), request.setupToken());
        device.markSeen();
        return DeviceResponse.from(devices.save(device));
    }

    @PostMapping("/api/device-events")
    public EventResponse event(@Valid @RequestBody DeviceEventRequest request) {
        IotDevice device = authenticate(request.deviceCode(), request.setupToken());
        device.markSeen(); devices.save(device);
        RfidCard card = cards.findByCardUidIgnoreCase(request.rfidUid()).filter(RfidCard::isActive)
                .orElseThrow(() -> new IllegalArgumentException("RFID card is not assigned"));
        UUID teacherId = card.getTeacher().getId();
        var activeSession = sessions.findAllBySessionDateOrderByStartsAt(java.time.LocalDate.now()).stream()
                .filter(s -> s.getClassrooms().stream().anyMatch(room -> room.getId().equals(device.getClassroom().getId())))
                .filter(s -> !s.getStartsAt().isAfter(java.time.LocalDateTime.now()) && !s.getEndsAt().isBefore(java.time.LocalDateTime.now()))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("No active session for this classroom"));
        AttendanceRecord record = records.findBySessionIdAndTeacherId(activeSession.getId(), teacherId).orElse(null);
        if (record == null) record = records.save(new AttendanceRecord(activeSession, card.getTeacher(), card.getCardUid()));
        else record.checkOut();
        return new EventResponse(device.getDeviceCode(), record.getStatus(), record.getId(), Instant.now());
    }

    private IotDevice authenticate(String code, String token) {
        IotDevice device = devices.findByDeviceCodeIgnoreCase(code).orElseThrow(() -> new IllegalArgumentException("Device not found"));
        if (device.getStatus() == DeviceStatus.DISABLED || !passwords.matches(token, device.getSetupTokenHash())) throw new IllegalArgumentException("Invalid device token");
        return device;
    }
    private static String randomToken() { byte[] bytes = new byte[24]; RANDOM.nextBytes(bytes); return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); }
    private static String escape(String value) { return value.replace("\\", "\\\\").replace("\"", "\\\""); }
    private static String firmwareTemplate() {
        return "/* ELNEB personalized firmware */\n#include <WiFi.h>\n#include <HTTPClient.h>\n#include <SPI.h>\n#include <MFRC522.h>\nconst char* WIFI_SSID = \"REPLACE_WITH_WIFI_SSID\";\nconst char* WIFI_PASSWORD = \"REPLACE_WITH_WIFI_PASSWORD\";\nconst char* DEVICE_ID = \"REPLACE_WITH_DEVICE_ID\";\nconst char* SETUP_TOKEN = \"REPLACE_WITH_SETUP_TOKEN\";\nconst char* API_BASE_URL = \"http://localhost:8080/api\";\nconstexpr uint8_t RFID_SS_PIN = 5; constexpr uint8_t RFID_RST_PIN = 22; constexpr uint8_t STATUS_LED_PIN = 2;\nMFRC522 rfid(RFID_SS_PIN, RFID_RST_PIN);\nvoid connectWifi(){ WiFi.begin(WIFI_SSID,WIFI_PASSWORD); unsigned long started=millis(); while(WiFi.status()!=WL_CONNECTED && millis()-started<30000){delay(500);digitalWrite(STATUS_LED_PIN,!digitalRead(STATUS_LED_PIN));} }\nvoid heartbeat(){ HTTPClient http; http.begin(String(API_BASE_URL)+\"/device/heartbeat\"); http.addHeader(\"Content-Type\",\"application/json\"); http.POST(String(\"{\\\"deviceCode\\\":\\\"\")+DEVICE_ID+\"\\\",\\\"setupToken\\\":\\\"\"+SETUP_TOKEN+\"\\\"}\"); http.end(); }\nvoid setup(){pinMode(STATUS_LED_PIN,OUTPUT);Serial.begin(115200);SPI.begin();rfid.PCD_Init();connectWifi();}\nvoid loop(){if(WiFi.status()!=WL_CONNECTED){connectWifi();return;} heartbeat(); if(!rfid.PICC_IsNewCardPresent()||!rfid.PICC_ReadCardSerial()){delay(100);return;} String uid; for(byte i=0;i<rfid.uid.size;i++){if(rfid.uid.uidByte[i]<0x10)uid+=\"0\";uid+=String(rfid.uid.uidByte[i],HEX);} HTTPClient http; http.begin(String(API_BASE_URL)+\"/device-events\"); http.addHeader(\"Content-Type\",\"application/json\"); int response=http.POST(String(\"{\\\"deviceCode\\\":\\\"\")+DEVICE_ID+\"\\\",\\\"setupToken\\\":\\\"\"+SETUP_TOKEN+\"\\\",\\\"rfidUid\\\":\\\"\"+uid+\"\\\"}\"); digitalWrite(STATUS_LED_PIN,response>=200&&response<300); http.end(); rfid.PICC_HaltA(); delay(1000);}\n";
    }
    public record ProvisionRequest(@NotNull UUID classroomId, @NotBlank String deviceCode) {}
    public record DeviceRequest(@NotBlank String deviceCode, @NotBlank String setupToken) {}
    public record DeviceEventRequest(@NotBlank String deviceCode, @NotBlank String setupToken, @NotBlank String rfidUid) {}
    public record DeviceResponse(UUID id, String deviceCode, DeviceStatus status, Instant lastSeenAt) {
        static DeviceResponse from(IotDevice d) {
            DeviceStatus status = d.getStatus();
            if (status == DeviceStatus.ONLINE && (d.getLastSeenAt() == null || d.getLastSeenAt().isBefore(Instant.now().minusSeconds(120)))) {
                status = DeviceStatus.OFFLINE;
            }
            return new DeviceResponse(d.getId(), d.getDeviceCode(), status, d.getLastSeenAt());
        }
    }
    public record ProvisionResponse(UUID id,String deviceCode,String setupToken,UUID classroomId){static ProvisionResponse from(IotDevice d,String token){return new ProvisionResponse(d.getId(),d.getDeviceCode(),token,d.getClassroom().getId());}}
    public record EventResponse(String deviceCode,String attendanceStatus,UUID attendanceId,Instant receivedAt){}
}
