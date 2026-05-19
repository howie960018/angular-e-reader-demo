package com.ctbc.ebookstore.repository;

import com.ctbc.ebookstore.bean.AppUser;
import com.ctbc.ebookstore.bean.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByUser(AppUser user);
    Optional<Cart> findByUserId(Long userId);
}
