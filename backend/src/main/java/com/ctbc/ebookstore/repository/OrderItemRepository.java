package com.ctbc.ebookstore.repository;

import com.ctbc.ebookstore.bean.AppUser;
import com.ctbc.ebookstore.bean.Book;
import com.ctbc.ebookstore.bean.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @Query("SELECT COUNT(oi) > 0 FROM OrderItem oi " +
           "WHERE oi.order.user = :user AND oi.book = :book AND oi.order.status = 'COMPLETED'")
    boolean hasPurchasedBook(@Param("user") AppUser user, @Param("book") Book book);

    @Query("SELECT DISTINCT oi.book FROM OrderItem oi " +
           "WHERE oi.order.user = :user AND oi.order.status = 'COMPLETED'")
    List<Book> findPurchasedBooksByUser(@Param("user") AppUser user);
}
