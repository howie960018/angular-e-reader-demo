package com.ctbc.ebookstore.bean;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "platform_transaction")
@Getter @Setter @NoArgsConstructor
public class PlatformTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "platform_wallet_id", nullable = false)
    private PlatformWallet platformWallet;

    @Column(nullable = false, length = 30)
    private String type;  // revenue_share

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amountPoints;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private BookOrder order;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public PlatformTransaction(PlatformWallet platformWallet, String type,
                               BigDecimal amountPoints, BookOrder order) {
        this.platformWallet = platformWallet;
        this.type = type;
        this.amountPoints = amountPoints;
        this.order = order;
        this.createdAt = LocalDateTime.now();
    }
}
