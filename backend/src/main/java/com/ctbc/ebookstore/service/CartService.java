package com.ctbc.ebookstore.service;

import com.ctbc.ebookstore.bean.AppUser;
import com.ctbc.ebookstore.bean.Book;
import com.ctbc.ebookstore.bean.Cart;
import com.ctbc.ebookstore.bean.CartItem;
import com.ctbc.ebookstore.exception.BadRequestException;
import com.ctbc.ebookstore.exception.ResourceNotFoundException;
import com.ctbc.ebookstore.repository.BookRepository;
import com.ctbc.ebookstore.repository.CartItemRepository;
import com.ctbc.ebookstore.repository.CartRepository;
import com.ctbc.ebookstore.repository.OrderItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class CartService {

    private final CartRepository cartRepo;
    private final CartItemRepository cartItemRepo;
    private final BookRepository bookRepo;
    private final OrderItemRepository orderItemRepo;

    public CartService(CartRepository cartRepo, CartItemRepository cartItemRepo,
                       BookRepository bookRepo, OrderItemRepository orderItemRepo) {
        this.cartRepo = cartRepo;
        this.cartItemRepo = cartItemRepo;
        this.bookRepo = bookRepo;
        this.orderItemRepo = orderItemRepo;
    }

    public Cart getOrCreateCart(AppUser user) {
        return cartRepo.findByUser(user).orElseGet(() -> {
            Cart cart = new Cart(user);
            return cartRepo.save(cart);
        });
    }

    @Transactional
    public Cart addItem(AppUser user, Long bookId, int quantity) {
        Cart cart = getOrCreateCart(user);
        Book book = bookRepo.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found: " + bookId));

        // 電子書不可重複購買
        if (orderItemRepo.hasPurchasedBook(user, book)) {
            throw new BadRequestException("您已購買過《" + book.getTitle() + "》，無需重複購買");
        }

        // 電子書每本只需一份，忽略傳入數量，固定 quantity = 1
        CartItem existing = cartItemRepo.findByCartAndBookId(cart, bookId).orElse(null);
        if (existing != null) {
            // 已在購物車中，直接回傳（不累加）
            return cart;
        }

        CartItem item = new CartItem(cart, book, 1);
        cart.getItems().add(item);
        cartItemRepo.save(item);
        cart.setUpdatedAt(LocalDateTime.now());
        return cartRepo.save(cart);
    }

    @Transactional
    public Cart updateItemQuantity(AppUser user, Long bookId, int quantity) {
        Cart cart = getOrCreateCart(user);

        CartItem item = cartItemRepo.findByCartAndBookId(cart, bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found for book: " + bookId));

        // 電子書數量固定為 1，quantity <= 0 視為移除
        if (quantity <= 0) {
            cart.getItems().remove(item);
            cartItemRepo.delete(item);
        }
        // quantity > 0 時不變更（ebook 永遠 1 份）

        cart.setUpdatedAt(LocalDateTime.now());
        return cartRepo.save(cart);
    }

    @Transactional
    public Cart removeItem(AppUser user, Long bookId) {
        Cart cart = getOrCreateCart(user);

        CartItem item = cartItemRepo.findByCartAndBookId(cart, bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found for book: " + bookId));

        cart.getItems().remove(item);
        cartItemRepo.delete(item);
        cart.setUpdatedAt(LocalDateTime.now());
        return cartRepo.save(cart);
    }

    @Transactional
    public Cart clearCart(AppUser user) {
        Cart cart = getOrCreateCart(user);
        cart.getItems().clear();
        cart.setUpdatedAt(LocalDateTime.now());
        return cartRepo.save(cart);
    }
}
