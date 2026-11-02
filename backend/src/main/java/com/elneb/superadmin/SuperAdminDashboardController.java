package com.elneb.superadmin;

import com.elneb.campus.Classroom;
import com.elneb.campus.ClassroomRepository;
import com.elneb.device.IotDevice;
import com.elneb.device.IotDeviceRepository;
import com.elneb.institution.Institution;
import com.elneb.institution.InstitutionRepository;
import com.elneb.subscription.*;
import com.elneb.tenant.Tenant;
import com.elneb.tenant.TenantRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/super-admin")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SuperAdminDashboardController {
    private final TenantRepository tenants;
    private final InstitutionRepository institutions;
    private final SubscriptionRepository subscriptions;
    private final IotDeviceRepository devices;
    private final ClassroomRepository classrooms;
    private final DiscountTokenRepository coupons;

    public SuperAdminDashboardController(TenantRepository tenants, InstitutionRepository institutions,
                                          SubscriptionRepository subscriptions, IotDeviceRepository devices,
                                          ClassroomRepository classrooms, DiscountTokenRepository coupons) {
        this.tenants = tenants; this.institutions = institutions; this.subscriptions = subscriptions;
        this.devices = devices; this.classrooms = classrooms; this.coupons = coupons;
    }

    @GetMapping("/dashboard")
    public Dashboard dashboard() {
        List<College> colleges = collegeRows();
        long active = colleges.stream().filter(row -> row.subscriptionStatus().equals("ACTIVE")).count();
        long trial = colleges.stream().filter(row -> row.subscriptionStatus().equals("TRIALING")).count();
        long expired = colleges.stream().filter(row -> row.subscriptionStatus().equals("EXPIRED") || row.subscriptionStatus().equals("UNSUBSCRIBED")).count();
        long online = devices.findAll().stream().filter(device -> device.getStatus().name().equals("ONLINE")).count();
        return new Dashboard(colleges.size(), active, trial, expired, classrooms.count(), devices.count(), online, coupons.count(), colleges);
    }

    @GetMapping("/colleges")
    public List<College> colleges() { return collegeRows(); }

    @GetMapping("/subscriptions")
    public List<SubscriptionRow> subscriptionRows() {
        return subscriptions.findAll().stream().map(subscription -> new SubscriptionRow(
                subscription.getTenant().getId(), subscription.getTenant().getName(),
                subscription.getPlan() == null ? "TRIAL" : subscription.getPlan().getName(),
                subscription.getStatus().name(), subscription.isTrial(), subscription.getStartsAt(), subscription.getEndsAt()
        )).sorted(Comparator.comparing(SubscriptionRow::endsAt)).toList();
    }

    @GetMapping("/coupons")
    public long couponCount() { return coupons.count(); }

    private List<College> collegeRows() {
        Map<UUID, Institution> byTenant = new HashMap<>();
        institutions.findAll().forEach(institution -> byTenant.putIfAbsent(institution.getTenant().getId(), institution));
        Map<UUID, Long> deviceCounts = new HashMap<>();
        devices.findAll().forEach(device -> {
            UUID tenantId = device.getClassroom().getFloor().getBuilding().getCampus().getInstitution().getTenant().getId();
            deviceCounts.merge(tenantId, 1L, Long::sum);
        });
        return tenants.findAll().stream().map(tenant -> {
            Subscription subscription = subscriptions.findTopByTenantIdOrderByEndsAtDesc(tenant.getId()).orElse(null);
            String status = subscription == null ? "UNSUBSCRIBED" : effectiveStatus(subscription);
            return new College(tenant.getId(), byTenant.get(tenant.getId()) == null ? tenant.getName() : byTenant.get(tenant.getId()).getName(),
                    tenant.getSlug(), tenant.isActive(), status, subscription == null ? null : subscription.getEndsAt(),
                    deviceCounts.getOrDefault(tenant.getId(), 0L));
        }).toList();
    }

    private static String effectiveStatus(Subscription subscription) {
        return subscription.getEndsAt().isBefore(Instant.now()) ? "EXPIRED" : subscription.getStatus().name();
    }

    public record Dashboard(long totalColleges, long activeSubscriptions, long trialColleges, long expiredColleges,
                            long totalClassrooms, long totalDevices, long onlineDevices, long totalCoupons,
                            List<College> colleges) {}
    public record College(UUID tenantId, String name, String slug, boolean active, String subscriptionStatus,
                          Instant subscriptionEndsAt, long deviceCount) {}
    public record SubscriptionRow(UUID tenantId, String collegeName, String plan, String status, boolean trial,
                                  Instant startsAt, Instant endsAt) {}
}
