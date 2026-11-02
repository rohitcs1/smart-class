package com.elneb.subscription;

import com.elneb.common.UuidEntitySupport;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "subscription_plans")
public class SubscriptionPlan extends UuidEntitySupport {
    @Column(nullable = false, unique = true, length = 100)
    private String name;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BillingPeriod billingPeriod;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal priceInr;
    @Column(nullable = false)
    private boolean active = true;

    protected SubscriptionPlan() {}
    public String getName() { return name; }
    public BillingPeriod getBillingPeriod() { return billingPeriod; }
    public BigDecimal getPriceInr() { return priceInr; }
    public boolean isActive() { return active; }
}
