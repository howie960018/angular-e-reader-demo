package com.ctbc.ebookstore.repository;

import com.ctbc.ebookstore.bean.Wallet;
import com.ctbc.ebookstore.bean.WalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {
    List<WalletTransaction> findByWalletOrderByCreatedAtDesc(Wallet wallet);
    List<WalletTransaction> findByWalletId(Long walletId);
}
