package com.ctbc.ebookstore.service;

import com.ctbc.ebookstore.bean.*;
import com.ctbc.ebookstore.dto.PaymentResult;
import com.ctbc.ebookstore.exception.BadRequestException;
import com.ctbc.ebookstore.exception.ResourceNotFoundException;
import com.ctbc.ebookstore.repository.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class WalletService {

    private final WalletRepository walletRepo;
    private final WalletTransactionRepository transactionRepo;
    private final TopUpCodeRepository topUpCodeRepo;
    private final TopUpCodeUsageRepository usageRepo;
    private final PlatformWalletRepository platformWalletRepo;
    private final PublisherWalletRepository publisherWalletRepo;
    private final PublisherTransactionRepository publisherTxRepo;
    private final PlatformTransactionRepository platformTxRepo;

    public WalletService(WalletRepository walletRepo,
                         WalletTransactionRepository transactionRepo,
                         TopUpCodeRepository topUpCodeRepo,
                         TopUpCodeUsageRepository usageRepo,
                         PlatformWalletRepository platformWalletRepo,
                         PublisherWalletRepository publisherWalletRepo,
                         PublisherTransactionRepository publisherTxRepo,
                         PlatformTransactionRepository platformTxRepo) {
        this.walletRepo = walletRepo;
        this.transactionRepo = transactionRepo;
        this.topUpCodeRepo = topUpCodeRepo;
        this.usageRepo = usageRepo;
        this.platformWalletRepo = platformWalletRepo;
        this.publisherWalletRepo = publisherWalletRepo;
        this.publisherTxRepo = publisherTxRepo;
        this.platformTxRepo = platformTxRepo;
    }

    // ──────────────────────────────────────────────────────────────
    // User Wallet
    // ──────────────────────────────────────────────────────────────

    public Wallet getOrCreateWallet(AppUser user) {
        return walletRepo.findByUser(user).orElseGet(() -> {
            String type = user.getRole().equals("SELLER") ? "seller" : "user";
            return walletRepo.save(new Wallet(user, type, BigDecimal.ZERO));
        });
    }

    public Wallet getUserWallet(AppUser user) {
        return walletRepo.findByUser(user).orElseGet(() -> {
            String type = user.getRole().equals("SELLER") ? "seller" : "user";
            return walletRepo.save(new Wallet(user, type, BigDecimal.ZERO));
        });
    }

    public BigDecimal getBalance(AppUser user) {
        return getUserWallet(user).getBalance();
    }

    public List<WalletTransaction> getTransactions(AppUser user) {
        Wallet wallet = getUserWallet(user);
        return transactionRepo.findByWalletOrderByCreatedAtDesc(wallet);
    }

    /**
     * 取得錢包並加 FOR UPDATE 鎖（確保先存在再鎖定）。
     * 必須在 @Transactional 方法內呼叫，鎖隨 transaction 釋放。
     */
    private Wallet getWalletLocked(AppUser user) {
        getOrCreateWallet(user); // 確保存在
        return walletRepo.findByUserForUpdate(user)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));
    }

    /** Admin 手動入款（DEPOSIT） */
    @Transactional
    public Wallet deposit(AppUser user, BigDecimal amount, String description) {
        Wallet wallet = getWalletLocked(user);
        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepo.save(wallet);
        transactionRepo.save(new WalletTransaction(wallet, "DEPOSIT", amount, description));
        return wallet;
    }

    /** 直接儲值（TOPUP，正數） */
    @Transactional
    public Wallet topup(AppUser user, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("儲值點數必須大於 0");
        }
        Wallet wallet = getWalletLocked(user);
        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepo.save(wallet);
        transactionRepo.save(new WalletTransaction(wallet, "TOPUP", amount, "直接儲值"));
        return wallet;
    }

    /**
     * 購買扣款（不帶 order 參照，舊版相容呼叫）。
     * amount 傳入正數，記錄時存為負數。
     */
    @Transactional
    public void purchase(AppUser user, BigDecimal amount, String description) {
        Wallet wallet = getWalletLocked(user);
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new BadRequestException("錢包餘額不足");
        }
        wallet.setBalance(wallet.getBalance().subtract(amount));
        walletRepo.save(wallet);
        transactionRepo.save(new WalletTransaction(wallet, "PURCHASE", amount.negate(), description));
    }

    /**
     * 購買扣款（帶 order 參照，購書流程使用）。
     * amount 傳入正數，記錄時存為負數。
     */
    @Transactional
    public void purchase(AppUser user, BigDecimal amount, BookOrder order) {
        Wallet wallet = getWalletLocked(user);
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new BadRequestException("錢包餘額不足");
        }
        wallet.setBalance(wallet.getBalance().subtract(amount));
        walletRepo.save(wallet);
        WalletTransaction tx = new WalletTransaction(
                wallet, "PURCHASE", amount.negate(), "電子書購買", order);
        transactionRepo.save(tx);
    }

    /** 兼容舊呼叫 */
    @Transactional
    public void payment(AppUser user, BigDecimal amount, String description) {
        purchase(user, amount, description);
    }

    /**
     * 兌換碼儲值。
     * 防護層：
     *  1. existsBy 快速排除已明確使用過的情況
     *  2. DB UNIQUE(code_id, user_id) 是最終防線
     *  3. 捕捉 DataIntegrityViolationException 處理極端並發
     */
    @Transactional
    public PaymentResult useTopUpCode(AppUser user, String code) {
        TopUpCode topUpCode = topUpCodeRepo.findByCode(code).orElse(null);
        if (topUpCode == null) {
            return new PaymentResult(false, "無效的兌換碼");
        }
        if (usageRepo.existsByTopUpCodeAndUser(topUpCode, user)) {
            return new PaymentResult(false, "您已經使用過此兌換碼");
        }

        try {
            usageRepo.save(new TopUpCodeUsage(topUpCode, user));
            usageRepo.flush(); // 立即觸發 INSERT，讓 unique constraint 在本 tx 內生效
        } catch (DataIntegrityViolationException e) {
            // 並發情境下另一個 request 搶先插入，此筆視為重複
            return new PaymentResult(false, "您已經使用過此兌換碼");
        }

        Wallet wallet = getWalletLocked(user);
        wallet.setBalance(wallet.getBalance().add(topUpCode.getAmount()));
        walletRepo.save(wallet);
        transactionRepo.save(new WalletTransaction(wallet, "TOPUP",
                topUpCode.getAmount(), "兌換碼: " + code));

        return new PaymentResult(true, "兌換成功", topUpCode.getAmount());
    }

    public List<TopUpCode> getAvailableCodesForUser(AppUser user) {
        return usageRepo.findCodesNotUsedByUser(user);
    }

    public List<TopUpCode> getAllCodes() {
        return topUpCodeRepo.findAll();
    }

    public long getUsageCount(TopUpCode code) {
        return usageRepo.countByTopUpCode(code);
    }

    public TopUpCode createTopUpCode(String code, BigDecimal amount) {
        if (topUpCodeRepo.findByCode(code).isPresent()) {
            throw new BadRequestException("兌換碼已存在: " + code);
        }
        return topUpCodeRepo.save(new TopUpCode(code, amount));
    }

    public void deleteTopUpCode(Long id) {
        TopUpCode code = topUpCodeRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("兌換碼不存在: " + id));
        topUpCodeRepo.delete(code);
    }

    // ──────────────────────────────────────────────────────────────
    // Platform Wallet
    // ──────────────────────────────────────────────────────────────

    public PlatformWallet getOrCreatePlatformWallet() {
        return platformWalletRepo.findFirstBy()
                .orElseGet(() -> platformWalletRepo.save(new PlatformWallet(BigDecimal.ZERO)));
    }

    public PlatformWallet getPlatformWallet() {
        return platformWalletRepo.findFirstBy()
                .orElseThrow(() -> new ResourceNotFoundException("Platform wallet not found"));
    }

    public List<PlatformTransaction> getPlatformTransactions() {
        PlatformWallet wallet = getOrCreatePlatformWallet();
        return platformTxRepo.findByPlatformWalletOrderByCreatedAtDesc(wallet);
    }

    // ──────────────────────────────────────────────────────────────
    // Publisher Wallet
    // ──────────────────────────────────────────────────────────────

    public PublisherWallet getOrCreatePublisherWallet(AppUser publisher) {
        return publisherWalletRepo.findByPublisher(publisher)
                .orElseGet(() -> publisherWalletRepo.save(new PublisherWallet(publisher)));
    }

    public List<PublisherWallet> getAllPublisherWallets() {
        return publisherWalletRepo.findAll();
    }

    public List<PublisherTransaction> getPublisherTransactions(AppUser publisher) {
        PublisherWallet wallet = getOrCreatePublisherWallet(publisher);
        return publisherTxRepo.findByPublisherWalletOrderByCreatedAtDesc(wallet);
    }
}
