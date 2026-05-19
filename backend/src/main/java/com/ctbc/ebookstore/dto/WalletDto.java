package com.ctbc.ebookstore.dto;

import com.ctbc.ebookstore.bean.Wallet;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter
public class WalletDto {
    private Long id;
    private Long userId;
    private String type;
    private BigDecimal balance;
    private LocalDateTime createdAt;

    public static WalletDto from(Wallet wallet) {
        WalletDto dto = new WalletDto();
        dto.setId(wallet.getId());
        dto.setUserId(wallet.getUser().getId());
        dto.setType(wallet.getType());
        dto.setBalance(wallet.getBalance());
        dto.setCreatedAt(wallet.getCreatedAt());
        return dto;
    }
}
