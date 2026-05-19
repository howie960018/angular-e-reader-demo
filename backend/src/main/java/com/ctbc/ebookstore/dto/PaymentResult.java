package com.ctbc.ebookstore.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter @AllArgsConstructor
public class PaymentResult {
    private boolean success;
    private String message;
    private BigDecimal amount;

    public PaymentResult(boolean success, String message) {
        this.success = success;
        this.message = message;
        this.amount = null;
    }
}
