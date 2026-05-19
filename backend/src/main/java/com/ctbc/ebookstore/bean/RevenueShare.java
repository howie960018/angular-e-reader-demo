package com.ctbc.ebookstore.bean;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "revenue_share")
@Getter @Setter @NoArgsConstructor
public class RevenueShare {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "order_id", nullable = false)
    private BookOrder order;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "publisher_id", nullable = false)
    private AppUser publisher;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPoints;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal platformSharePoints;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal publisherSharePoints;

    @Column(nullable = false)
    private boolean settled = false;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime settledAt;

    public RevenueShare(BookOrder order, Book book, AppUser publisher,
                        BigDecimal totalPoints, BigDecimal platformSharePoints,
                        BigDecimal publisherSharePoints) {
        this.order = order;
        this.book = book;
        this.publisher = publisher;
        this.totalPoints = totalPoints;
        this.platformSharePoints = platformSharePoints;
        this.publisherSharePoints = publisherSharePoints;
        this.createdAt = LocalDateTime.now();
    }
}
