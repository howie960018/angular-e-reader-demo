package com.ctbc.ebookstore.bean;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "wallet_transaction")
@Getter @Setter @NoArgsConstructor
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    /** topup / purchase / deposit */
    @Column(nullable = false, length = 20)
    private String type;

    /** 正數 = 入帳（topup），負數 = 扣款（purchase） */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(length = 255)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private BookOrder order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id")
    private Book book;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public WalletTransaction(Wallet wallet, String type, BigDecimal amount, String description) {
        this.wallet = wallet;
        this.type = type;
        this.amount = amount;
        this.description = description;
        this.createdAt = LocalDateTime.now();
    }

    public WalletTransaction(Wallet wallet, String type, BigDecimal amount,
                             String description, BookOrder order) {
        this(wallet, type, amount, description);
        this.order = order;
    }
}
