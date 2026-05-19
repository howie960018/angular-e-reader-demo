package com.ctbc.ebookstore.dto;

import com.ctbc.ebookstore.bean.PlatformTransaction;
import com.ctbc.ebookstore.bean.PlatformWallet;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Getter @Setter
public class PlatformWalletDto {
    private Long id;
    private BigDecimal balancePoints;
    private List<PlatformTransactionDto> transactions;

    public static PlatformWalletDto from(PlatformWallet wallet, List<PlatformTransaction> txList) {
        PlatformWalletDto dto = new PlatformWalletDto();
        dto.setId(wallet.getId());
        dto.setBalancePoints(wallet.getBalancePoints());
        dto.setTransactions(txList.stream()
                .map(PlatformTransactionDto::from)
                .collect(Collectors.toList()));
        return dto;
    }
}
