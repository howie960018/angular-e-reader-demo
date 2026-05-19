package com.ctbc.ebookstore.repository;

import com.ctbc.ebookstore.bean.PlatformWallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlatformWalletRepository extends JpaRepository<PlatformWallet, Long> {
    /** 取得第一筆（全系統只有一筆平台錢包） */
    Optional<PlatformWallet> findFirstBy();
}
