package com.ctbc.ebookstore.dto;

import com.ctbc.ebookstore.bean.PublisherTransaction;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter
public class PublisherTransactionDto {
    private Long id;
    private String type;
    private BigDecimal amountPoints;
    private Long orderId;
    private LocalDateTime createdAt;

    public static PublisherTransactionDto from(PublisherTransaction tx) {
        PublisherTransactionDto dto = new PublisherTransactionDto();
        dto.setId(tx.getId());
        dto.setType(tx.getType());
        dto.setAmountPoints(tx.getAmountPoints());
        dto.setOrderId(tx.getOrder() != null ? tx.getOrder().getId() : null);
        dto.setCreatedAt(tx.getCreatedAt());
        return dto;
    }
}
