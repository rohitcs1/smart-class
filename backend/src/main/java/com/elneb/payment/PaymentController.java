package com.elneb.payment;

import com.elneb.subscription.SubscriptionPlan;
import com.elneb.subscription.SubscriptionPlanRepository;
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

@RestController
@RequestMapping("/api/payments/razorpay")
public class PaymentController {
    private final RazorpayProperties properties;
    private final SubscriptionPlanRepository plans;
    private final RestClient client;

    public PaymentController(RazorpayProperties properties, SubscriptionPlanRepository plans, RestClient.Builder builder) {
        this.properties = properties;
        this.plans = plans;
        this.client = builder.baseUrl("https://api.razorpay.com").build();
    }

    @PostMapping("/orders")
    public OrderResponse createOrder(@Valid @RequestBody CreateOrderRequest request) {
        if (!properties.enabled() || isBlank(properties.keyId()) || isBlank(properties.keySecret())) {
            throw new IllegalStateException("Razorpay is not configured. Set RAZORPAY_ENABLED, RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET.");
        }
        SubscriptionPlan plan = plans.findById(request.planId()).filter(SubscriptionPlan::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Subscription plan not found"));
        long amountPaise = plan.getPriceInr().movePointRight(2).longValueExact();
        Map<?, ?> response = client.post().uri("/v1/orders")
                .headers(headers -> headers.setBasicAuth(properties.keyId(), properties.keySecret()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("amount", amountPaise, "currency", properties.currency(), "receipt", "ELNEB-" + UUID.randomUUID()))
                .retrieve().body(Map.class);
        if (response == null || response.get("id") == null) throw new IllegalStateException("Razorpay returned an invalid order response");
        return new OrderResponse(response.get("id").toString(), amountPaise, properties.currency(), properties.keyId(),
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
        return new VerificationResponse(true, request.razorpayPaymentId());
    }

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

    public record CreateOrderRequest(@NotNull UUID planId, @NotBlank String customerName, @NotBlank @Email String customerEmail) {}
    public record VerifyRequest(@NotBlank String razorpayOrderId, @NotBlank String razorpayPaymentId, @NotBlank String razorpaySignature) {}
    public record OrderResponse(String orderId, long amount, String currency, String keyId, String customerName, String customerEmail) {}
    public record VerificationResponse(boolean verified, String paymentId) {}
}
