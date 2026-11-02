package com.elneb.subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {
    Optional<Subscription> findTopByTenantIdOrderByEndsAtDesc(UUID tenantId);
    List<Subscription> findAllByEndsAtBeforeAndStatusNot(java.time.Instant endsAt, SubscriptionStatus status);
}
