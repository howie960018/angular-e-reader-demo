package com.ctbc.ebookstore.service;

import com.ctbc.ebookstore.bean.*;
import com.ctbc.ebookstore.exception.BadRequestException;
import com.ctbc.ebookstore.exception.ResourceNotFoundException;
import com.ctbc.ebookstore.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepo;
    private final CartService cartService;
    private final WalletService walletService;
    private final SettlementService settlementService;

    public OrderService(OrderRepository orderRepo, CartService cartService,
                        WalletService walletService, SettlementService settlementService) {
        this.orderRepo = orderRepo;
        this.cartService = cartService;
        this.walletService = walletService;
        this.settlementService = settlementService;
    }

    public List<BookOrder> getUserOrders(AppUser user) {
        return orderRepo.findByUser(user);
    }

    public List<BookOrder> getAllOrders() {
        return orderRepo.findAllByOrderByCreatedAtDesc();
    }

    public BookOrder findById(Long id) {
        return orderRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
    }

    @Transactional
    public BookOrder createFromCart(AppUser user) {
        Cart cart = cartService.getOrCreateCart(user);

        if (cart.getItems().isEmpty()) {
            throw new BadRequestException("Cart is empty");
        }

        BigDecimal total = cart.getItems().stream()
                .map(item -> item.getBook().getPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 1. 先檢查餘額（不扣款），避免訂單建立後才發現餘額不足
        Wallet wallet = walletService.getUserWallet(user);
        if (wallet.getBalance().compareTo(total) < 0) {
            throw new BadRequestException("錢包餘額不足");
        }

        // 2. 建立訂單
        BookOrder order = new BookOrder();
        order.setUser(user);
        order.setTotalPrice(total);
        order.setStatus("COMPLETED");

        for (CartItem cartItem : cart.getItems()) {
            OrderItem orderItem = new OrderItem(order, cartItem.getBook(),
                    cartItem.getBook().getPrice(), cartItem.getQuantity());
            order.getItems().add(orderItem);
        }

        BookOrder saved = orderRepo.save(order);

        // 3. 從使用者錢包扣款，帶 order 參照（記錄負數金額）
        walletService.purchase(user, total, saved);

        // 4. 每本書建立一筆待結算分潤記錄（此時平台/出版商尚未入帳）
        for (CartItem cartItem : cart.getItems()) {
            BigDecimal bookTotal = cartItem.getBook().getPrice()
                    .multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            settlementService.createPendingShare(
                    saved,
                    cartItem.getBook(),
                    cartItem.getBook().getSeller(),
                    bookTotal
            );
        }

        // 5. 清空購物車
        cartService.clearCart(user);

        return saved;
    }

    @Transactional
    public BookOrder updateStatus(Long orderId, String status) {
        BookOrder order = findById(orderId);
        order.setStatus(status);
        order.setUpdatedAt(LocalDateTime.now());
        return orderRepo.save(order);
    }

    public void delete(Long orderId) {
        BookOrder order = findById(orderId);
        orderRepo.delete(order);
    }
}
