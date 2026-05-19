package com.ctbc.ebookstore.dto;

import com.ctbc.ebookstore.bean.WalletTransaction;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter
public class WalletTransactionDto {
    private Long id;
    private Long walletId;
    private String type;
    private BigDecimal amount;
    private String description;
    private Long orderId;
    private Long bookId;
    private LocalDateTime createdAt;

    public static WalletTransactionDto from(WalletTransaction tx) {
        WalletTransactionDto dto = new WalletTransactionDto();
        dto.setId(tx.getId());
        dto.setWalletId(tx.getWallet().getId());
        dto.setType(tx.getType());
        dto.setAmount(tx.getAmount());
        dto.setDescription(tx.getDescription());
        dto.setOrderId(tx.getOrder() != null ? tx.getOrder().getId() : null);
        dto.setBookId(tx.getBook() != null ? tx.getBook().getId() : null);
        dto.setCreatedAt(tx.getCreatedAt());
        return dto;
    }
}
