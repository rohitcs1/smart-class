package com.elneb.tenant;

import com.elneb.common.UuidEntitySupport;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "tenants")
public class Tenant extends UuidEntitySupport {
    @Column(nullable = false, length = 150)
    private String name;
    @Column(nullable = false, unique = true, length = 120)
    private String slug;
    @Column(nullable = false)
    private boolean active = true;
    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected Tenant() {}
    public Tenant(String name, String slug) {
        this.name = name.trim();
        this.slug = slug.trim().toLowerCase();
    }
    public String getName() { return name; }
    public String getSlug() { return slug; }
    public boolean isActive() { return active; }
}
