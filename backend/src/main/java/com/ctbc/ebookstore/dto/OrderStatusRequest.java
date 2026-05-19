package com.ctbc.ebookstore.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class OrderStatusRequest {
    private String status;  // PENDING, COMPLETED, CANCELLED
}
