package com.elneb.subscription;

import com.elneb.common.UuidEntitySupport;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "discount_tokens")
public class DiscountToken extends UuidEntitySupport {
    @Column(nullable = false, unique = true, length = 100)
    private String code;
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal discountPercent;
    @Column(nullable = false, length = 20)
    private String appliesTo;
    @Column(nullable = false)
    private boolean active = true;
    private Instant expiresAt;
    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected DiscountToken() {}
    public DiscountToken(String code, BigDecimal discountPercent, String appliesTo, Instant expiresAt) {
        this.code = code;
        this.discountPercent = discountPercent;
        this.appliesTo = appliesTo;
        this.expiresAt = expiresAt;
    }
    public String getCode() { return code; }
    public BigDecimal getDiscountPercent() { return discountPercent; }
    public String getAppliesTo() { return appliesTo; }
    public boolean isActive() { return active; }
    public Instant getExpiresAt() { return expiresAt; }
}
