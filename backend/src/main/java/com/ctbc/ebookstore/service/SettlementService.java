package com.ctbc.ebookstore.service;

import com.ctbc.ebookstore.bean.*;
import com.ctbc.ebookstore.dto.RevenueShareDto;
import com.ctbc.ebookstore.dto.SettlementSummary;
import com.ctbc.ebookstore.exception.ResourceNotFoundException;
import com.ctbc.ebookstore.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SettlementService {

    /** 平台抽成比例 30% */
    private static final BigDecimal PLATFORM_RATE = new BigDecimal("0.30");

    private final RevenueShareRepository revenueShareRepo;
    private final PlatformWalletRepository platformWalletRepo;
    private final PlatformTransactionRepository platformTxRepo;
    private final PublisherWalletRepository publisherWalletRepo;
    private final PublisherTransactionRepository publisherTxRepo;

    public SettlementService(RevenueShareRepository revenueShareRepo,
                             PlatformWalletRepository platformWalletRepo,
                             PlatformTransactionRepository platformTxRepo,
                             PublisherWalletRepository publisherWalletRepo,
                             PublisherTransactionRepository publisherTxRepo) {
        this.revenueShareRepo = revenueShareRepo;
        this.platformWalletRepo = platformWalletRepo;
        this.platformTxRepo = platformTxRepo;
        this.publisherWalletRepo = publisherWalletRepo;
        this.publisherTxRepo = publisherTxRepo;
    }

    public List<RevenueShareDto> getPending() {
        return revenueShareRepo.findBySettledFalseOrderByCreatedAtDesc()
                .stream().map(RevenueShareDto::from).collect(Collectors.toList());
    }

    public List<RevenueShareDto> getHistory() {
        return revenueShareRepo.findBySettledTrueOrderBySettledAtDesc()
                .stream().map(RevenueShareDto::from).collect(Collectors.toList());
    }

    public List<RevenueShareDto> getPublisherRevenue(AppUser publisher) {
        return revenueShareRepo.findByPublisherOrderByCreatedAtDesc(publisher)
                .stream().map(RevenueShareDto::from).collect(Collectors.toList());
    }

    /**
     * 執行結算：將所有未結算的 revenue_share 入帳至平台錢包與出版商錢包。
     */
    /**
     * 執行結算。
     * 防止雙重結算的三層鎖定策略：
     *  1. findBySettledFalseForUpdate()  → 鎖定所有待結算記錄
     *     第二個並發請求阻塞，第一筆 commit 後看到空列表直接回傳
     *  2. findAllForUpdate()             → 鎖定平台錢包
     *  3. findByPublisherForUpdate()     → 鎖定各出版商錢包
     */
    @Transactional
    public SettlementSummary settle() {
        // 以 FOR UPDATE 讀取，同時鎖住所有待結算記錄
        List<RevenueShare> pending = revenueShareRepo.findBySettledFalseForUpdate();

        if (pending.isEmpty()) {
            return new SettlementSummary(0, BigDecimal.ZERO, BigDecimal.ZERO, "無待結算項目");
        }

        // 鎖定（或建立）平台錢包
        PlatformWallet platformWallet = platformWalletRepo.findAllForUpdate()
                .stream().findFirst()
                .orElseGet(() -> platformWalletRepo.save(new PlatformWallet(BigDecimal.ZERO)));

        BigDecimal totalPlatform = BigDecimal.ZERO;
        BigDecimal totalPublisher = BigDecimal.ZERO;
        LocalDateTime now = LocalDateTime.now();

        for (RevenueShare rs : pending) {
            BookOrder order = rs.getOrder();

            // ── 平台錢包入帳 ──────────────────────────────────────
            platformWallet.setBalancePoints(
                    platformWallet.getBalancePoints().add(rs.getPlatformSharePoints()));
            platformTxRepo.save(new PlatformTransaction(
                    platformWallet, "revenue_share",
                    rs.getPlatformSharePoints(), order));

            // ── 出版商錢包入帳（FOR UPDATE 鎖定）────────────────────
            PublisherWallet pubWallet = publisherWalletRepo
                    .findByPublisherForUpdate(rs.getPublisher())
                    .orElseGet(() -> publisherWalletRepo.save(
                            new PublisherWallet(rs.getPublisher())));
            pubWallet.setBalancePoints(
                    pubWallet.getBalancePoints().add(rs.getPublisherSharePoints()));
            publisherWalletRepo.save(pubWallet);
            publisherTxRepo.save(new PublisherTransaction(
                    pubWallet, "revenue_share",
                    rs.getPublisherSharePoints(), order));

            totalPlatform = totalPlatform.add(rs.getPlatformSharePoints());
            totalPublisher = totalPublisher.add(rs.getPublisherSharePoints());

            rs.setSettled(true);
            rs.setSettledAt(now);
            revenueShareRepo.save(rs);
        }

        platformWalletRepo.save(platformWallet);

        return new SettlementSummary(
                pending.size(),
                totalPlatform,
                totalPublisher,
                "成功結算 " + pending.size() + " 筆，平台入帳 " + totalPlatform
                        + " 點，出版商合計入帳 " + totalPublisher + " 點"
        );
    }

    /**
     * 建立分潤紀錄（購書時呼叫，此時不入帳，只產生待結算記錄）。
     * 平台 30%，出版商 70%。
     */
    public RevenueShare createPendingShare(BookOrder order, Book book,
                                           AppUser publisher, BigDecimal totalPoints) {
        BigDecimal platform = totalPoints.multiply(PLATFORM_RATE)
                .setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal publisherShare = totalPoints.subtract(platform);
        RevenueShare rs = new RevenueShare(order, book, publisher,
                totalPoints, platform, publisherShare);
        return revenueShareRepo.save(rs);
    }
}
