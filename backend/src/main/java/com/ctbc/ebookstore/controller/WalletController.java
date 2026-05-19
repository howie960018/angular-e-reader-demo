package com.ctbc.ebookstore.controller;

import com.ctbc.ebookstore.bean.AppUser;
import com.ctbc.ebookstore.bean.PlatformWallet;
import com.ctbc.ebookstore.bean.PublisherWallet;
import com.ctbc.ebookstore.bean.TopUpCode;
import com.ctbc.ebookstore.dto.*;
import com.ctbc.ebookstore.service.AppUserService;
import com.ctbc.ebookstore.service.WalletService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    private final WalletService walletService;
    private final AppUserService userService;

    public WalletController(WalletService walletService, AppUserService userService) {
        this.walletService = walletService;
        this.userService = userService;
    }

    // ──────────────────────────────────────────────────────────────
    // User Wallet
    // ──────────────────────────────────────────────────────────────

    @GetMapping
    public WalletDto getWallet(Authentication auth) {
        AppUser user = userService.findByUsername(auth.getName());
        return WalletDto.from(walletService.getUserWallet(user));
    }

    @GetMapping("/balance")
    public Map<String, Object> getBalance(Authentication auth) {
        AppUser user = userService.findByUsername(auth.getName());
        return Map.of("balance", walletService.getBalance(user));
    }

    @GetMapping("/transactions")
    public List<WalletTransactionDto> getTransactions(Authentication auth) {
        AppUser user = userService.findByUsername(auth.getName());
        return walletService.getTransactions(user).stream()
                .map(WalletTransactionDto::from)
                .collect(Collectors.toList());
    }

    /** 使用兌換碼儲值 */
    @PostMapping("/topup")
    public PaymentResult useTopUpCode(@Valid @RequestBody TopUpCodeRequest req, Authentication auth) {
        AppUser user = userService.findByUsername(auth.getName());
        return walletService.useTopUpCode(user, req.getCode());
    }

    /** 直接儲值（輸入點數，模擬金流入款） */
    @PostMapping("/topup-direct")
    public WalletDto topupDirect(@RequestBody Map<String, Object> body, Authentication auth) {
        AppUser user = userService.findByUsername(auth.getName());
        java.math.BigDecimal amount = new java.math.BigDecimal(body.get("amount").toString());
        return WalletDto.from(walletService.topup(user, amount));
    }

    /** Admin：手動對指定使用者存款 */
    @PostMapping("/deposit")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public WalletDto deposit(@RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(body.get("userId").toString());
        java.math.BigDecimal amount = new java.math.BigDecimal(body.get("amount").toString());
        String description = body.getOrDefault("description", "管理員入款").toString();
        AppUser targetUser = userService.findById(userId);
        return WalletDto.from(walletService.deposit(targetUser, amount, description));
    }

    // ──────────────────────────────────────────────────────────────
    // Top-Up Codes
    // ──────────────────────────────────────────────────────────────

    @GetMapping("/topup-codes/available")
    public List<TopUpCodeDto> getAvailableCodes(Authentication auth) {
        AppUser user = userService.findByUsername(auth.getName());
        return walletService.getAvailableCodesForUser(user).stream()
                .map(c -> TopUpCodeDto.from(c, 0L))
                .collect(Collectors.toList());
    }

    @GetMapping("/topup-codes")
    @PreAuthorize("hasRole('ADMIN')")
    public List<TopUpCodeDto> getAllCodes() {
        return walletService.getAllCodes().stream()
                .map(c -> TopUpCodeDto.from(c, walletService.getUsageCount(c)))
                .collect(Collectors.toList());
    }

    @PostMapping("/topup-codes")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public TopUpCodeDto createTopUpCode(@Valid @RequestBody CreateTopUpCodeRequest req) {
        TopUpCode code = walletService.createTopUpCode(req.getCode(), req.getAmount());
        return TopUpCodeDto.from(code, 0L);
    }

    @DeleteMapping("/topup-codes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteTopUpCode(@PathVariable Long id) {
        walletService.deleteTopUpCode(id);
    }

    // ──────────────────────────────────────────────────────────────
    // Platform Wallet  (Admin)
    // ──────────────────────────────────────────────────────────────

    /** Admin：取得平台錢包餘額與交易紀錄 */
    @GetMapping("/platform")
    @PreAuthorize("hasRole('ADMIN')")
    public PlatformWalletDto getPlatformWallet() {
        PlatformWallet wallet = walletService.getOrCreatePlatformWallet();
        return PlatformWalletDto.from(wallet, walletService.getPlatformTransactions());
    }

    // ──────────────────────────────────────────────────────────────
    // Publisher Wallet  (Seller / Admin)
    // ──────────────────────────────────────────────────────────────

    /** Seller：取得自己的出版商分潤錢包 */
    @GetMapping("/publisher/me")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public PublisherWalletDto getMyPublisherWallet(Authentication auth) {
        AppUser user = userService.findByUsername(auth.getName());
        PublisherWallet wallet = walletService.getOrCreatePublisherWallet(user);
        return PublisherWalletDto.from(wallet, walletService.getPublisherTransactions(user));
    }

    /** Admin：取得所有出版商錢包（摘要，不含交易明細） */
    @GetMapping("/publishers")
    @PreAuthorize("hasRole('ADMIN')")
    public List<PublisherWalletDto> getAllPublisherWallets() {
        return walletService.getAllPublisherWallets().stream()
                .map(PublisherWalletDto::fromNoTx)
                .collect(Collectors.toList());
    }

    /** Admin：取得指定出版商錢包（含交易明細） */
    @GetMapping("/publishers/{publisherId}")
    @PreAuthorize("hasRole('ADMIN')")
    public PublisherWalletDto getPublisherWallet(@PathVariable Long publisherId) {
        AppUser publisher = userService.findById(publisherId);
        PublisherWallet wallet = walletService.getOrCreatePublisherWallet(publisher);
        return PublisherWalletDto.from(wallet, walletService.getPublisherTransactions(publisher));
    }
}
