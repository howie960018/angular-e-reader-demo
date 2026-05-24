package com.ctbc.ebookstore.repository;

import com.ctbc.ebookstore.bean.Book;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookRepository extends JpaRepository<Book, Long> {
    List<Book> findByStatus(String status);
    List<Book> findByCategoryIdAndStatus(Long categoryId, String status);
    List<Book> findBySellerIdAndStatus(Long sellerId, String status);
    List<Book> findBySellerId(Long sellerId);
}
