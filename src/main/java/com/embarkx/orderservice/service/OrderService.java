package com.embarkx.orderservice.service;

import com.embarkx.orderservice.dto.CreateOrderRequest;
import com.embarkx.orderservice.dto.OrderResponse;
import com.embarkx.orderservice.model.Order;
import com.embarkx.orderservice.model.OrderStatus;
import com.embarkx.orderservice.model.Product;
import com.embarkx.orderservice.repository.OrderRepository;
import com.embarkx.orderservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final PaymentService paymentService;
    private final StringRedisTemplate redisTemplate;

    @Transactional
    public OrderResponse createOrder(String idempotencyKey, CreateOrderRequest request) {
        // 1. Idempotency Check using Redis
        Boolean isNewKey = redisTemplate.opsForValue()
                .setIfAbsent("idempotency:" + idempotencyKey, "LOCKED", Duration.ofMinutes(10));

        if (Boolean.FALSE.equals(isNewKey)) {
            Optional<Order> existingOrder = orderRepository.findByIdempotencyKey(idempotencyKey);
            if (existingOrder.isPresent()) {
                Order order = existingOrder.get();
                return OrderResponse.builder()
                        .orderId(order.getId())
                        .productId(order.getProductId())
                        .quantity(order.getQuantity())
                        .totalAmount(order.getTotalAmount())
                        .status(order.getStatus())
                        .message("Duplicate request processed (Idempotent response)")
                        .build();
            }
        }

        // 2. Fetch Product with Pessimistic Lock (Prevents Race Condition)
        Product product = productRepository.findByIdWithPessimisticLock(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (product.getStockQuantity() < request.getQuantity()) {
            throw new RuntimeException("Insufficient Stock!");
        }

        // 3. Deduct Stock
        product.setStockQuantity(product.getStockQuantity() - request.getQuantity());
        productRepository.save(product);

        BigDecimal totalAmount = product.getPrice().multiply(BigDecimal.valueOf(request.getQuantity()));

        // 4. Save Order as PENDING
        Order order = Order.builder()
                .idempotencyKey(idempotencyKey)
                .productId(product.getId())
                .quantity(request.getQuantity())
                .totalAmount(totalAmount)
                .status(OrderStatus.PENDING)
                .build();
        
        order = orderRepository.save(order);

        // 5. Process Payment via Circuit Breaker
        boolean paymentSuccess = paymentService.processPayment(order.getId(), totalAmount);

        if (paymentSuccess) {
            order.setStatus(OrderStatus.COMPLETED);
            orderRepository.save(order);
            return OrderResponse.builder()
                    .orderId(order.getId())
                    .productId(order.getProductId())
                    .quantity(order.getQuantity())
                    .totalAmount(order.getTotalAmount())
                    .status(OrderStatus.COMPLETED)
                    .message("Order placed successfully")
                    .build();
        } else {
            // Rollback stock if payment fails
            product.setStockQuantity(product.getStockQuantity() + request.getQuantity());
            productRepository.save(product);

            order.setStatus(OrderStatus.PAYMENT_FAILED);
            orderRepository.save(order);

            return OrderResponse.builder()
                    .orderId(order.getId())
                    .productId(order.getProductId())
                    .quantity(order.getQuantity())
                    .totalAmount(order.getTotalAmount())
                    .status(OrderStatus.PAYMENT_FAILED)
                    .message("Payment failed or Circuit Breaker triggered")
                    .build();
        }
    }
}