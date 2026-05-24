package com.ctbc.ebookstore.service;

import com.ctbc.ebookstore.bean.*;
import com.ctbc.ebookstore.exception.BadRequestException;
import com.ctbc.ebookstore.exception.ForbiddenException;
import com.ctbc.ebookstore.exception.ResourceNotFoundException;
import com.ctbc.ebookstore.repository.OrderRepository;
import org.springframework.scheduling.annotation.Scheduled;
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

    /**
     * 從購物車建立訂單（狀態：PENDING，尚未付款）。
     * 不扣款、不建立分潤，僅鎖定商品並清空購物車。
     * 訂單有效期限 5 分鐘，逾時自動取消。
     */
    @Transactional
    public BookOrder createFromCart(AppUser user) {
        Cart cart = cartService.getOrCreateCartForUpdate(user);

        if (cart.getItems().isEmpty()) {
            throw new BadRequestException("購物車是空的");
        }

        BigDecimal total = cart.getItems().stream()
                .map(item -> item.getBook().getPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BookOrder order = new BookOrder();
        order.setUser(user);
        order.setTotalPrice(total);
        order.setStatus("PENDING");
        order.setExpiresAt(LocalDateTime.now().plusMinutes(5));

        for (CartItem cartItem : cart.getItems()) {
            OrderItem orderItem = new OrderItem(order, cartItem.getBook(),
                    cartItem.getBook().getPrice(), cartItem.getQuantity());
            order.getItems().add(orderItem);
        }

        BookOrder saved = orderRepo.save(order);
        cartService.clearCart(user);
        return saved;
    }

    /**
     * 使用者確認付款：扣除錢包餘額並將訂單標記為 COMPLETED。
     * 同時建立分潤記錄供管理員後續結算。
     */
    @Transactional
    public BookOrder confirmPayment(Long orderId, AppUser user) {
        BookOrder order = findById(orderId);

        if (!order.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("無權操作此訂單");
        }
        if (!"PENDING".equals(order.getStatus())) {
            throw new BadRequestException("此訂單狀態無法付款");
        }
        if (LocalDateTime.now().isAfter(order.getExpiresAt())) {
            order.setStatus("CANCELLED");
            order.setUpdatedAt(LocalDateTime.now());
            orderRepo.save(order);
            throw new BadRequestException("訂單已逾時，請重新下單");
        }

        walletService.purchase(user, order.getTotalPrice(), order);

        for (OrderItem item : order.getItems()) {
            settlementService.createPendingShare(
                    order,
                    item.getBook(),
                    item.getBook().getSeller(),
                    item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
            );
        }

        order.setStatus("COMPLETED");
        order.setUpdatedAt(LocalDateTime.now());
        return orderRepo.save(order);
    }

    /**
     * 使用者主動取消訂單（僅限 PENDING 狀態）。
     */
    @Transactional
    public BookOrder cancelOrder(Long orderId, AppUser user) {
        BookOrder order = findById(orderId);

        if (!order.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("無權操作此訂單");
        }
        if (!"PENDING".equals(order.getStatus())) {
            throw new BadRequestException("只有待付款訂單可以取消");
        }

        order.setStatus("CANCELLED");
        order.setUpdatedAt(LocalDateTime.now());
        return orderRepo.save(order);
    }

    /**
     * 每 60 秒掃描一次，將逾時未付款的 PENDING 訂單自動取消。
     */
    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void cancelExpiredOrders() {
        List<BookOrder> expired = orderRepo.findByStatusAndExpiresAtBefore("PENDING", LocalDateTime.now());
        for (BookOrder order : expired) {
            order.setStatus("CANCELLED");
            order.setUpdatedAt(LocalDateTime.now());
        }
        if (!expired.isEmpty()) {
            orderRepo.saveAll(expired);
        }
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
