package com.ctbc.ebookstore.bean;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "top_up_code_usage", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"code_id", "user_id"})
})
@Getter @Setter @NoArgsConstructor
public class TopUpCodeUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "code_id", nullable = false)
    private TopUpCode topUpCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(nullable = false)
    private LocalDateTime usedAt = LocalDateTime.now();

    public TopUpCodeUsage(TopUpCode topUpCode, AppUser user) {
        this.topUpCode = topUpCode;
        this.user = user;
        this.usedAt = LocalDateTime.now();
    }
}
