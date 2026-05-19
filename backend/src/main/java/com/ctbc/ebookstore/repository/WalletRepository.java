package com.ctbc.ebookstore.repository;

import com.ctbc.ebookstore.bean.AppUser;
import com.ctbc.ebookstore.bean.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {
    Optional<Wallet> findByUser(AppUser user);
    Optional<Wallet> findByUserId(Long userId);
    Optional<Wallet> findByType(String type);
}
