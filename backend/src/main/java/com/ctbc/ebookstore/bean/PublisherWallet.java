package com.ctbc.ebookstore.bean;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "publisher_wallet")
@Getter @Setter @NoArgsConstructor
public class PublisherWallet {

    /** 與出版商 user_id 共用同一個 PK */
    @Id
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @MapsId
    @JoinColumn(name = "publisher_id")
    private AppUser publisher;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal balancePoints = BigDecimal.ZERO;

    @OneToMany(mappedBy = "publisherWallet", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PublisherTransaction> transactions = new ArrayList<>();

    public PublisherWallet(AppUser publisher) {
        this.publisher = publisher;
        this.balancePoints = BigDecimal.ZERO;
    }
}
