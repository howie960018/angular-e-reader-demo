package com.ctbc.ebookstore.repository;

import com.ctbc.ebookstore.bean.PublisherTransaction;
import com.ctbc.ebookstore.bean.PublisherWallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PublisherTransactionRepository extends JpaRepository<PublisherTransaction, Long> {
    List<PublisherTransaction> findByPublisherWalletOrderByCreatedAtDesc(PublisherWallet wallet);
}
