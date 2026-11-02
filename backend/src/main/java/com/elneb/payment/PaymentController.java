package com.elneb.payment;

import com.elneb.subscription.SubscriptionPlan;
import com.elneb.subscription.SubscriptionPlanRepository;
import com.elneb.subscription.SubscriptionLifecycleService;
import com.elneb.subscription.DiscountToken;
import com.elneb.subscription.DiscountTokenRepository;
import com.elneb.auth.User;
import com.elneb.auth.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.core.context.SecurityContextHolder;

@RestController
@RequestMapping("/api/payments/razorpay")
public class PaymentController {
    private final RazorpayProperties properties;
    private final SubscriptionPlanRepository plans;
    private final RestClient client;
    private final PaymentOrderRepository orders;
    private final UserRepository users;
    private final SubscriptionLifecycleService lifecycle;
    private final DiscountTokenRepository discountTokens;

    public PaymentController(RazorpayProperties properties, SubscriptionPlanRepository plans, RestClient.Builder builder,
                             PaymentOrderRepository orders, UserRepository users, SubscriptionLifecycleService lifecycle,
                             DiscountTokenRepository discountTokens) {
        this.properties = properties;
        this.plans = plans;
        this.client = builder.baseUrl("https://api.razorpay.com").build();
        this.orders=orders; this.users=users; this.lifecycle=lifecycle; this.discountTokens=discountTokens;
    }

    @PostMapping("/orders")
    public OrderResponse createOrder(@Valid @RequestBody CreateOrderRequest request) {
        if (!properties.enabled() || isBlank(properties.keyId()) || isBlank(properties.keySecret())) {
            throw new IllegalStateException("Razorpay is not configured. Set RAZORPAY_ENABLED, RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET.");
        }
        SubscriptionPlan plan = plans.findById(request.planId()).filter(SubscriptionPlan::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Subscription plan not found"));
        BigDecimal amount = plan.getPriceInr();
        if (request.discountCode() != null && !request.discountCode().isBlank()) {
            DiscountToken token = discountTokens.findByCodeIgnoreCase(request.discountCode().trim())
                    .filter(DiscountToken::isActive)
                    .orElseThrow(() -> new IllegalArgumentException("Discount token is invalid or inactive"));
            if (token.getExpiresAt() != null && token.getExpiresAt().isBefore(java.time.Instant.now())) throw new IllegalArgumentException("Discount token has expired");
            if (!token.getAppliesTo().equals("BOTH") && !token.getAppliesTo().equals(plan.getBillingPeriod().name())) throw new IllegalArgumentException("Discount token does not apply to this plan");
            amount = amount.multiply(BigDecimal.ONE.subtract(token.getDiscountPercent().movePointLeft(2))).setScale(2, java.math.RoundingMode.HALF_UP);
        }
        if (amount.compareTo(BigDecimal.ONE) < 0) amount = BigDecimal.ONE;
        long amountPaise = amount.movePointRight(2).longValueExact();
        Map<?, ?> response = client.post().uri("/v1/orders")
                .headers(headers -> headers.setBasicAuth(properties.keyId(), properties.keySecret()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("amount", amountPaise, "currency", properties.currency(), "receipt", "ELNEB-" + UUID.randomUUID()))
                .retrieve().body(Map.class);
        if (response == null || response.get("id") == null) throw new IllegalStateException("Razorpay returned an invalid order response");
        String orderId=response.get("id").toString();
        User user=currentUser();
        if(user.getTenant()==null) throw new IllegalArgumentException("Only an institution account can purchase a subscription");
        orders.save(new PaymentOrder(user.getTenant(),plan,orderId,amountPaise));
        return new OrderResponse(orderId, amountPaise, properties.currency(), properties.keyId(),
                request.customerName(), request.customerEmail());
    }

    @PostMapping("/verify")
    public VerificationResponse verify(@Valid @RequestBody VerifyRequest request) {
        if (isBlank(properties.keySecret())) throw new IllegalStateException("Razorpay secret is not configured");
        String payload = request.razorpayOrderId() + "|" + request.razorpayPaymentId();
        String expected = hmacSha256(payload, properties.keySecret());
        if (!expected.equalsIgnoreCase(request.razorpaySignature())) {
            throw new IllegalArgumentException("Razorpay payment signature is invalid");
        }
        PaymentOrder order=orders.findByRazorpayOrderId(request.razorpayOrderId()).orElseThrow(()->new IllegalArgumentException("Payment order not found"));
        if(!order.getStatus().equals("PAID")) { order.markPaid(request.razorpayPaymentId()); orders.save(order); lifecycle.activate(order.getTenant(),order.getPlan()); }
        return new VerificationResponse(true, request.razorpayPaymentId());
    }

    @PostMapping(value="/webhook", consumes=MediaType.APPLICATION_JSON_VALUE)
    public WebhookResponse webhook(@RequestHeader(value="X-Razorpay-Signature",required=false) String signature,
                                   @RequestBody String payload) {
        if(properties.webhookSecret()==null || properties.webhookSecret().isBlank()) throw new IllegalStateException("Razorpay webhook secret is not configured");
        if(signature==null || !hmacSha256(payload,properties.webhookSecret()).equalsIgnoreCase(signature)) throw new IllegalArgumentException("Razorpay webhook signature is invalid");
        String orderId=jsonValue(payload,"order_id"); String paymentId=jsonValue(payload,"payment_id");
        if(orderId!=null) orders.findByRazorpayOrderId(orderId).ifPresent(order->{if(!order.getStatus().equals("PAID")){order.markPaid(paymentId);orders.save(order);lifecycle.activate(order.getTenant(),order.getPlan());}});
        return new WebhookResponse(true);
    }

    private User currentUser(){String id=SecurityContextHolder.getContext().getAuthentication().getName();return users.findById(UUID.fromString(id)).orElseThrow(()->new IllegalArgumentException("User account not found"));}
    private static String jsonValue(String json,String key){java.util.regex.Matcher m=java.util.regex.Pattern.compile("\""+key+"\"\\s*:\\s*\"([^\"]*)\"").matcher(json);return m.find()?m.group(1):null;}

    private static String hmacSha256(String value, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to verify Razorpay signature", ex);
        }
    }

    private static boolean isBlank(String value) { return value == null || value.isBlank(); }

    public record CreateOrderRequest(@NotNull UUID planId, String discountCode, @NotBlank String customerName, @NotBlank @Email String customerEmail) {}
    public record VerifyRequest(@NotBlank String razorpayOrderId, @NotBlank String razorpayPaymentId, @NotBlank String razorpaySignature) {}
    public record OrderResponse(String orderId, long amount, String currency, String keyId, String customerName, String customerEmail) {}
    public record VerificationResponse(boolean verified, String paymentId) {}
    public record WebhookResponse(boolean received) {}
}
