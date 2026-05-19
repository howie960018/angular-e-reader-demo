package com.ctbc.ebookstore.bean;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "platform_wallet")
@Getter @Setter @NoArgsConstructor
public class PlatformWallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal balancePoints = BigDecimal.ZERO;

    @OneToMany(mappedBy = "platformWallet", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PlatformTransaction> transactions = new ArrayList<>();

    public PlatformWallet(BigDecimal initialBalance) {
        this.balancePoints = initialBalance;
    }
}
