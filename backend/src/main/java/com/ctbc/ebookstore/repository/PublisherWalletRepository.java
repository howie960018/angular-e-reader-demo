package com.ctbc.ebookstore.repository;

import com.ctbc.ebookstore.bean.AppUser;
import com.ctbc.ebookstore.bean.PublisherWallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PublisherWalletRepository extends JpaRepository<PublisherWallet, Long> {
    Optional<PublisherWallet> findByPublisher(AppUser publisher);
}
