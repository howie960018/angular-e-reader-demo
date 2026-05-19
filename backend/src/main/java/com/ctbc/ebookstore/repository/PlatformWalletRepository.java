package com.ctbc.ebookstore.repository;

import com.ctbc.ebookstore.bean.PlatformWallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PlatformWalletRepository extends JpaRepository<PlatformWallet, Long> {
    /** 取得第一筆（全系統只有一筆平台錢包） */
    Optional<PlatformWallet> findFirstBy();

    /** SELECT ... FOR UPDATE：結算時鎖定平台錢包，防止並發結算雙重入帳 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT pw FROM PlatformWallet pw ORDER BY pw.id ASC")
    List<PlatformWallet> findAllForUpdate();
}
