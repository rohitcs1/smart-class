package com.elneb.subscription;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.elneb.auth.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;

@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {
    private final SubscriptionPlanRepository plans;
    private final DiscountTokenRepository tokens;
    private final SubscriptionRepository subscriptions;
    private final UserRepository users;

    public SubscriptionController(SubscriptionPlanRepository plans, DiscountTokenRepository tokens, SubscriptionRepository subscriptions, UserRepository users) {
        this.plans = plans; this.tokens = tokens; this.subscriptions=subscriptions; this.users=users;
    }

    @GetMapping("/status")
    public StatusResponse status() {
        var user=users.findById(UUID.fromString(SecurityContextHolder.getContext().getAuthentication().getName())).orElseThrow(() -> new IllegalArgumentException("User account not found"));
        if(user.getTenant()==null) return new StatusResponse("PLATFORM",false,true,null,null,null);
        var subscription=subscriptions.findTopByTenantIdOrderByEndsAtDesc(user.getTenant().getId()).orElse(null);
        if(subscription==null) return new StatusResponse("UNSUBSCRIBED",false,false,null,null,null);
        boolean active=subscription.getEndsAt().isAfter(Instant.now()) && subscription.getStatus()!=SubscriptionStatus.EXPIRED;
        return new StatusResponse(active?subscription.getStatus().name():"EXPIRED",active,!active,subscription.getStartsAt(),subscription.getEndsAt(),subscription.isTrial());
    }

    @GetMapping("/plans")
    public List<PlanResponse> plans() {
        return plans.findAllByActiveTrueOrderByPriceInr().stream()
                .map(p -> new PlanResponse(p.getId(), p.getName(), p.getBillingPeriod(), p.getPriceInr()))
                .toList();
    }

    @PostMapping("/discount-tokens")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public DiscountResponse createToken(@Valid @RequestBody CreateDiscountRequest request) {
        if (tokens.existsByCodeIgnoreCase(request.code())) throw new IllegalArgumentException("Discount token already exists");
        DiscountToken token = tokens.save(new DiscountToken(request.code().trim().toUpperCase(),
                request.discountPercent(), request.appliesTo().name(), request.expiresAt()));
        return DiscountResponse.from(token);
    }

    @PostMapping("/quote")
    public QuoteResponse quote(@Valid @RequestBody QuoteRequest request) {
        SubscriptionPlan plan = plans.findById(request.planId())
                .filter(SubscriptionPlan::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Subscription plan not found"));
        BigDecimal discount = BigDecimal.ZERO;
        if (request.discountCode() != null && !request.discountCode().isBlank()) {
            DiscountToken token = tokens.findByCodeIgnoreCase(request.discountCode().trim())
                    .filter(DiscountToken::isActive)
                    .orElseThrow(() -> new IllegalArgumentException("Discount token is invalid or inactive"));
            if (token.getExpiresAt() != null && token.getExpiresAt().isBefore(Instant.now())) {
                throw new IllegalArgumentException("Discount token has expired");
            }
            if (!token.getAppliesTo().equals("BOTH") && !token.getAppliesTo().equals(plan.getBillingPeriod().name())) {
                throw new IllegalArgumentException("Discount token does not apply to this plan");
            }
            discount = token.getDiscountPercent();
        }
        BigDecimal finalAmount = plan.getPriceInr().multiply(BigDecimal.ONE.subtract(discount.movePointLeft(2)))
                .setScale(2, RoundingMode.HALF_UP);
        if (finalAmount.compareTo(BigDecimal.ONE) < 0) finalAmount = BigDecimal.ONE;
        return new QuoteResponse(plan.getId(), plan.getPriceInr(), discount, finalAmount, "INR");
    }

    public enum AppliesTo { MONTHLY, YEARLY, BOTH }
    public record CreateDiscountRequest(@NotBlank String code, @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal discountPercent,
                                        @NotNull AppliesTo appliesTo, Instant expiresAt) {}
    public record QuoteRequest(@NotNull UUID planId, String discountCode) {}
    public record PlanResponse(UUID id, String name, BillingPeriod billingPeriod, BigDecimal priceInr) {}
    public record QuoteResponse(UUID planId, BigDecimal baseAmount, BigDecimal discountPercent, BigDecimal payableAmount, String currency) {}
    public record StatusResponse(String status, boolean monitoringEnabled, boolean historicalViewOnly, Instant startsAt, Instant endsAt, Boolean trial) {}
    public record DiscountResponse(UUID id, String code, BigDecimal discountPercent, String appliesTo, boolean active, Instant expiresAt) {
        static DiscountResponse from(DiscountToken token) {
            return new DiscountResponse(token.getId(), token.getCode(), token.getDiscountPercent(), token.getAppliesTo(),
                    token.isActive(), token.getExpiresAt());
        }
    }
}
