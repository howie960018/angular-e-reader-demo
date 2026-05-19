package com.ctbc.ebookstore.repository;

import com.ctbc.ebookstore.bean.AppUser;
import com.ctbc.ebookstore.bean.PublisherWallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PublisherWalletRepository extends JpaRepository<PublisherWallet, Long> {
    Optional<PublisherWallet> findByPublisher(AppUser publisher);

    /** SELECT ... FOR UPDATE：結算時鎖定出版商錢包，防止並發結算雙重入帳 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT pw FROM PublisherWallet pw WHERE pw.publisher = :publisher")
    Optional<PublisherWallet> findByPublisherForUpdate(@Param("publisher") AppUser publisher);
}
