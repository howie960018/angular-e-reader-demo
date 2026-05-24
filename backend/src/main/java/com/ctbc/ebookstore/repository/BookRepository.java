package com.ctbc.ebookstore.repository;

import com.ctbc.ebookstore.bean.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookRepository extends JpaRepository<Book, Long> {
    List<Book> findByStatus(String status);
    Page<Book> findByStatus(String status, Pageable pageable);
    List<Book> findByCategoryIdAndStatus(Long categoryId, String status);
    Page<Book> findByCategoryIdAndStatus(Long categoryId, String status, Pageable pageable);
    List<Book> findBySellerIdAndStatus(Long sellerId, String status);
    Page<Book> findBySellerIdAndStatus(Long sellerId, String status, Pageable pageable);
    List<Book> findBySellerId(Long sellerId);
    Page<Book> findBySellerId(Long sellerId, Pageable pageable);
}
