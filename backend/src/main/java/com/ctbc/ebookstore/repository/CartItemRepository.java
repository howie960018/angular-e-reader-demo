package com.ctbc.ebookstore.repository;

import com.ctbc.ebookstore.bean.Cart;
import com.ctbc.ebookstore.bean.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    Optional<CartItem> findByCartAndBookId(Cart cart, Long bookId);
    void deleteByCartAndBookId(Cart cart, Long bookId);
}
