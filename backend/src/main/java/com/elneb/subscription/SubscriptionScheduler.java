package com.elneb.subscription;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
@Component
public class SubscriptionScheduler {
    private final SubscriptionLifecycleService lifecycle;
    public SubscriptionScheduler(SubscriptionLifecycleService lifecycle){this.lifecycle=lifecycle;}
    @Scheduled(fixedDelay=60000)
    public void expireDue(){lifecycle.expireDueSubscriptions();}
}
