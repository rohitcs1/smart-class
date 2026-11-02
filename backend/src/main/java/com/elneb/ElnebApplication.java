package com.elneb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.elneb.payment.RazorpayProperties;
import org.springframework.web.client.RestClient;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties(RazorpayProperties.class)
@EnableScheduling
public class ElnebApplication {
    @org.springframework.context.annotation.Bean
    RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
    public static void main(String[] args) {
        SpringApplication.run(ElnebApplication.class, args);
    }
}
