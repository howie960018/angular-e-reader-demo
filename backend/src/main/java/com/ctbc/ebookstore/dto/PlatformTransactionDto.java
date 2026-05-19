package com.ctbc.ebookstore.dto;

import com.ctbc.ebookstore.bean.PlatformTransaction;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter
public class PlatformTransactionDto {
    private Long id;
    private String type;
    private BigDecimal amountPoints;
    private Long orderId;
    private LocalDateTime createdAt;

    public static PlatformTransactionDto from(PlatformTransaction tx) {
        PlatformTransactionDto dto = new PlatformTransactionDto();
        dto.setId(tx.getId());
        dto.setType(tx.getType());
        dto.setAmountPoints(tx.getAmountPoints());
        dto.setOrderId(tx.getOrder() != null ? tx.getOrder().getId() : null);
        dto.setCreatedAt(tx.getCreatedAt());
        return dto;
    }
}
