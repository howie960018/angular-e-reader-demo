package com.ctbc.ebookstore.dto;

import com.ctbc.ebookstore.bean.PublisherTransaction;
import com.ctbc.ebookstore.bean.PublisherWallet;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Getter @Setter
public class PublisherWalletDto {
    private Long publisherId;
    private String publisherName;
    private BigDecimal balancePoints;
    private List<PublisherTransactionDto> transactions;

    public static PublisherWalletDto from(PublisherWallet wallet, List<PublisherTransaction> txList) {
        PublisherWalletDto dto = new PublisherWalletDto();
        dto.setPublisherId(wallet.getPublisher().getId());
        dto.setPublisherName(wallet.getPublisher().getUsername());
        dto.setBalancePoints(wallet.getBalancePoints());
        dto.setTransactions(txList.stream()
                .map(PublisherTransactionDto::from)
                .collect(Collectors.toList()));
        return dto;
    }

    public static PublisherWalletDto fromNoTx(PublisherWallet wallet) {
        PublisherWalletDto dto = new PublisherWalletDto();
        dto.setPublisherId(wallet.getPublisher().getId());
        dto.setPublisherName(wallet.getPublisher().getUsername());
        dto.setBalancePoints(wallet.getBalancePoints());
        return dto;
    }
}
