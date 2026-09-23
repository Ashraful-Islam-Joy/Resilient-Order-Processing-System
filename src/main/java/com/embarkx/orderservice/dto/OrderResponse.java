package com.embarkx.orderservice.dto;

import com.embarkx.orderservice.model.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
public class OrderResponse {
    private Long orderId;
    private Long productId;
    private Integer quantity;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private String message;
}