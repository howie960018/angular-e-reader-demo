package com.ctbc.ebookstore.repository;

import com.ctbc.ebookstore.bean.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query("SELECT b FROM Book b WHERE b.status = 'active' AND " +
           "(LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(b.author) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Book> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT b FROM Book b WHERE b.status = 'active' AND b.category.id = :categoryId AND " +
           "(LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(b.author) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Book> searchByCategoryAndKeyword(@Param("categoryId") Long categoryId, @Param("keyword") String keyword, Pageable pageable);
}
