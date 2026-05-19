package com.ctbc.ebookstore.repository;

import com.ctbc.ebookstore.bean.AppUser;
import com.ctbc.ebookstore.bean.RevenueShare;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RevenueShareRepository extends JpaRepository<RevenueShare, Long> {
    List<RevenueShare> findBySettledFalseOrderByCreatedAtDesc();
    List<RevenueShare> findBySettledTrueOrderBySettledAtDesc();
    List<RevenueShare> findByPublisherOrderByCreatedAtDesc(AppUser publisher);
    List<RevenueShare> findByPublisherAndSettledOrderByCreatedAtDesc(AppUser publisher, boolean settled);

    /**
     * SELECT ... FOR UPDATE：結算時鎖定所有待結算記錄。
     * 第二個並發結算請求會阻塞，待第一筆 commit 後看到空列表，
     * 避免雙重結算。
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT rs FROM RevenueShare rs WHERE rs.settled = false ORDER BY rs.createdAt DESC")
    List<RevenueShare> findBySettledFalseForUpdate();
}
