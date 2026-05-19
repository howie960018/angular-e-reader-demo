package com.ctbc.ebookstore.repository;

import com.ctbc.ebookstore.bean.PlatformTransaction;
import com.ctbc.ebookstore.bean.PlatformWallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlatformTransactionRepository extends JpaRepository<PlatformTransaction, Long> {
    List<PlatformTransaction> findByPlatformWalletOrderByCreatedAtDesc(PlatformWallet wallet);
}
