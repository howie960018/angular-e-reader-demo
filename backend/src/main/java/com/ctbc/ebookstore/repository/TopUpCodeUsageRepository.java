package com.ctbc.ebookstore.repository;

import com.ctbc.ebookstore.bean.AppUser;
import com.ctbc.ebookstore.bean.TopUpCode;
import com.ctbc.ebookstore.bean.TopUpCodeUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface TopUpCodeUsageRepository extends JpaRepository<TopUpCodeUsage, Long> {
    boolean existsByTopUpCodeAndUser(TopUpCode topUpCode, AppUser user);
    List<TopUpCodeUsage> findByUser(AppUser user);
    long countByTopUpCode(TopUpCode topUpCode);

    @Query("SELECT c FROM TopUpCode c WHERE c.id NOT IN " +
           "(SELECT u.topUpCode.id FROM TopUpCodeUsage u WHERE u.user = :user)")
    List<TopUpCode> findCodesNotUsedByUser(@Param("user") AppUser user);
}
