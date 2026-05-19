package com.ctbc.ebookstore.repository;

import com.ctbc.ebookstore.bean.AppUser;
import com.ctbc.ebookstore.bean.RevenueShare;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RevenueShareRepository extends JpaRepository<RevenueShare, Long> {
    List<RevenueShare> findBySettledFalseOrderByCreatedAtDesc();
    List<RevenueShare> findBySettledTrueOrderBySettledAtDesc();
    List<RevenueShare> findByPublisherOrderByCreatedAtDesc(AppUser publisher);
    List<RevenueShare> findByPublisherAndSettledOrderByCreatedAtDesc(AppUser publisher, boolean settled);
}
