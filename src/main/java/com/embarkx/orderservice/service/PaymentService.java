package com.embarkx.orderservice.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Random;

@Service
public class PaymentService {

    private final Random random = new Random();

    @CircuitBreaker(name = "paymentService", fallbackMethod = "paymentFallback")
    public boolean processPayment(Long orderId, BigDecimal amount) {
        // Simulate external payment gateway call / failure
        if (random.nextInt(10) < 3) { // 30% chance of failure to test Circuit Breaker
            throw new RuntimeException("Payment Gateway Unreachable!");
        }
        return true;
    }

    public boolean paymentFallback(Long orderId, BigDecimal amount, Throwable throwable) {
        System.out.println("Circuit Breaker OPEN or Payment Failed! Fallback triggered for Order ID: " + orderId);
        return false;
    }
}