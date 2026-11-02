package com.elneb.auth;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.UUID;
import com.elneb.tenant.Tenant;

@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(name = "uk_users_email", columnNames = "email"))
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.BINARY)
    private UUID id;
    @Column(nullable = false, length = 190)
    private String email;
    @Column(nullable = false)
    private String passwordHash;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Role role;
    @Column(nullable = false)
    private boolean active = true;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;
    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected User() {}
    public User(String email, String passwordHash, Role role) {
        this.email = email.toLowerCase().trim();
        this.passwordHash = passwordHash;
        this.role = role;
    }
    public void assignTenant(Tenant tenant) { this.tenant = tenant; }
    public Tenant getTenant() { return tenant; }
    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public Role getRole() { return role; }
    public boolean isActive() { return active; }
    public void changeRole(Role role) { this.role = role; }
}
