package com.elneb.payment;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "elneb.payment.razorpay")
public record RazorpayProperties(
        boolean enabled,
        String keyId,
        String keySecret,
        String webhookSecret,
        String currency) {
}
