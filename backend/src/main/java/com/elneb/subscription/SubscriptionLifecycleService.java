package com.elneb.subscription;

import com.elneb.tenant.Tenant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;

@Service
public class SubscriptionLifecycleService {
    private final SubscriptionRepository subscriptions;
    public SubscriptionLifecycleService(SubscriptionRepository subscriptions){this.subscriptions=subscriptions;}
    @Transactional
    public Subscription createTrial(Tenant tenant) {
        Instant now=Instant.now();
        return subscriptions.save(new Subscription(tenant,null,SubscriptionStatus.TRIALING,now,now.plus(15, java.time.temporal.ChronoUnit.DAYS),true));
    }
    @Transactional
    public Subscription activate(Tenant tenant, SubscriptionPlan plan) {
        Instant now=Instant.now();
        Instant start=now; var current=subscriptions.findTopByTenantIdOrderByEndsAtDesc(tenant.getId()).orElse(null);
        if(current != null && current.getEndsAt().isAfter(now) && current.getStatus() != SubscriptionStatus.EXPIRED) start=current.getEndsAt();
        Instant end=start.plus(plan.getBillingPeriod()==BillingPeriod.MONTHLY ? 30 : 365, java.time.temporal.ChronoUnit.DAYS);
        Subscription target=current == null ? new Subscription(tenant,plan,SubscriptionStatus.ACTIVE,start,end,false) : current;
        target.activate(plan,start,end);
        return subscriptions.save(target);
    }
    @Transactional
    public void expireDueSubscriptions() {
        subscriptions.findAllByEndsAtBeforeAndStatusNot(Instant.now(),SubscriptionStatus.EXPIRED).forEach(s -> {s.expire();subscriptions.save(s);});
    }
}
