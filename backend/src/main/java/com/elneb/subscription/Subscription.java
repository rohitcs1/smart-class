package com.elneb.subscription;

import com.elneb.common.UuidEntitySupport;
import com.elneb.tenant.Tenant;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "subscriptions")
public class Subscription extends UuidEntitySupport {
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "tenant_id")
    private Tenant tenant;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "plan_id")
    private SubscriptionPlan plan;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private SubscriptionStatus status;
    @Column(name = "starts_at", nullable = false) private Instant startsAt;
    @Column(name = "ends_at", nullable = false) private Instant endsAt;
    @Column(nullable = false) private boolean trial;
    protected Subscription() {}
    public Subscription(Tenant tenant, SubscriptionPlan plan, SubscriptionStatus status, Instant startsAt, Instant endsAt, boolean trial) {
        this.tenant=tenant; this.plan=plan; this.status=status; this.startsAt=startsAt; this.endsAt=endsAt; this.trial=trial;
    }
    public Tenant getTenant(){return tenant;} public SubscriptionPlan getPlan(){return plan;} public SubscriptionStatus getStatus(){return status;}
    public Instant getStartsAt(){return startsAt;} public Instant getEndsAt(){return endsAt;} public boolean isTrial(){return trial;}
    public void activate(SubscriptionPlan plan, Instant startsAt, Instant endsAt){this.plan=plan;this.status=SubscriptionStatus.ACTIVE;this.startsAt=startsAt;this.endsAt=endsAt;this.trial=false;}
    public void expire(){if(status != SubscriptionStatus.CANCELLED) status=SubscriptionStatus.EXPIRED;}
}
