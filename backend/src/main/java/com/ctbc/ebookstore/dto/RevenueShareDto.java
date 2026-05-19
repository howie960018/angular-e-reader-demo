package com.ctbc.ebookstore.dto;

import com.ctbc.ebookstore.bean.RevenueShare;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter
public class RevenueShareDto {
    private Long id;
    private Long orderId;
    private Long bookId;
    private String bookTitle;
    private Long publisherId;
    private String publisherName;
    private BigDecimal totalPoints;
    private BigDecimal platformSharePoints;
    private BigDecimal publisherSharePoints;
    private boolean settled;
    private LocalDateTime createdAt;
    private LocalDateTime settledAt;

    public static RevenueShareDto from(RevenueShare rs) {
        RevenueShareDto dto = new RevenueShareDto();
        dto.setId(rs.getId());
        dto.setOrderId(rs.getOrder().getId());
        dto.setBookId(rs.getBook().getId());
        dto.setBookTitle(rs.getBook().getTitle());
        dto.setPublisherId(rs.getPublisher().getId());
        dto.setPublisherName(rs.getPublisher().getUsername());
        dto.setTotalPoints(rs.getTotalPoints());
        dto.setPlatformSharePoints(rs.getPlatformSharePoints());
        dto.setPublisherSharePoints(rs.getPublisherSharePoints());
        dto.setSettled(rs.isSettled());
        dto.setCreatedAt(rs.getCreatedAt());
        dto.setSettledAt(rs.getSettledAt());
        return dto;
    }
}
