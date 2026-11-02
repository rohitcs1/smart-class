package com.elneb.device;

import com.elneb.campus.Classroom;
import com.elneb.common.UuidEntitySupport;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "iot_devices")
public class IotDevice extends UuidEntitySupport {
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "classroom_id", nullable = false)
    private Classroom classroom;
    @Column(name = "device_code", nullable = false, unique = true, length = 100)
    private String deviceCode;
    @Column(name = "setup_token_hash", nullable = false, length = 255)
    private String setupTokenHash;
    @Column(name = "device_secret_hash", length = 255)
    private String deviceSecretHash;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DeviceStatus status = DeviceStatus.PROVISIONED;
    @Column(name = "last_seen_at")
    private Instant lastSeenAt;
    protected IotDevice() {}
    public IotDevice(Classroom classroom, String deviceCode, String setupTokenHash) {
        this.classroom = classroom; this.deviceCode = deviceCode; this.setupTokenHash = setupTokenHash;
    }
    public Classroom getClassroom() { return classroom; }
    public String getDeviceCode() { return deviceCode; }
    public String getSetupTokenHash() { return setupTokenHash; }
    public String getDeviceSecretHash() { return deviceSecretHash; }
    public DeviceStatus getStatus() { return status; }
    public Instant getLastSeenAt() { return lastSeenAt; }
    public void markSeen() { lastSeenAt = Instant.now(); status = DeviceStatus.ONLINE; }
    public void disable() { status = DeviceStatus.DISABLED; }
}
