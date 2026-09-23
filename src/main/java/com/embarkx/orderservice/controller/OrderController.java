package com.embarkx.orderservice.controller;

import com.embarkx.orderservice.dto.CreateOrderRequest;
import com.embarkx.orderservice.dto.OrderResponse;
import com.embarkx.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody CreateOrderRequest request) {

        OrderResponse response = orderService.createOrder(idempotencyKey, request);
        return ResponseEntity.ok(response);
    }
}