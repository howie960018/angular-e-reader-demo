package com.ctbc.ebookstore.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter @AllArgsConstructor
public class SettlementSummary {
    private int count;
    private BigDecimal totalPlatformPoints;
    private BigDecimal totalPublisherPoints;
    private String message;
}
