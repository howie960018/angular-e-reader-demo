package com.ctbc.ebookstore.repository;

import com.ctbc.ebookstore.bean.AppUser;
import com.ctbc.ebookstore.bean.BookOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository extends JpaRepository<BookOrder, Long> {
    List<BookOrder> findByUser(AppUser user);
    Page<BookOrder> findByUser(AppUser user, Pageable pageable);
    List<BookOrder> findByUserId(Long userId);
    List<BookOrder> findAllByOrderByCreatedAtDesc();
    List<BookOrder> findByStatusAndExpiresAtBefore(String status, LocalDateTime now);
}
