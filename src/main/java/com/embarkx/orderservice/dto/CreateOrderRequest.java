package com.embarkx.orderservice.dto;

import lombok.Data;

@Data
public class CreateOrderRequest {
    private Long productId;
    private Integer quantity;
}