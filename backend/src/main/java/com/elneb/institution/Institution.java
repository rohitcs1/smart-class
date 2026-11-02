package com.elneb.institution;

import com.elneb.common.UuidEntitySupport;
import com.elneb.tenant.Tenant;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "institutions")
public class Institution extends UuidEntitySupport {
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;
    @Column(nullable = false, length = 200)
    private String name;
    @Column(nullable = false, length = 80)
    private String code;
    @Column(length = 500)
    private String address;
    @Column(nullable = false)
    private boolean active = true;
    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected Institution() {}
    public Institution(Tenant tenant, String name, String code, String address) {
        this.tenant = tenant;
        this.name = name.trim();
        this.code = code.trim().toUpperCase();
        this.address = address;
    }
    public Tenant getTenant() { return tenant; }
    public String getName() { return name; }
    public String getCode() { return code; }
    public String getAddress() { return address; }
    public boolean isActive() { return active; }
}
