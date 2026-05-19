package com.ctbc.ebookstore.dto;

import com.ctbc.ebookstore.bean.TopUpCode;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter
public class TopUpCodeDto {
    private Long id;
    private String code;
    private BigDecimal amount;
    private long usageCount;
    private LocalDateTime createdAt;

    public static TopUpCodeDto from(TopUpCode code, long usageCount) {
        TopUpCodeDto dto = new TopUpCodeDto();
        dto.setId(code.getId());
        dto.setCode(code.getCode());
        dto.setAmount(code.getAmount());
        dto.setUsageCount(usageCount);
        dto.setCreatedAt(code.getCreatedAt());
        return dto;
    }

    public static TopUpCodeDto from(TopUpCode code) {
        return from(code, 0L);
    }
}
