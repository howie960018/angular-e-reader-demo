package com.ctbc.ebookstore.controller;

import com.ctbc.ebookstore.bean.AppUser;
import com.ctbc.ebookstore.bean.BookOrder;
import com.ctbc.ebookstore.dto.OrderDto;
import com.ctbc.ebookstore.dto.OrderStatusRequest;
import com.ctbc.ebookstore.service.AppUserService;
import com.ctbc.ebookstore.service.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final AppUserService userService;

    public OrderController(OrderService orderService, AppUserService userService) {
        this.orderService = orderService;
        this.userService = userService;
    }

    @GetMapping
    public Page<OrderDto> getUserOrders(Authentication auth,
                                        @PageableDefault(size = 20) Pageable pageable) {
        AppUser user = userService.findByUsername(auth.getName());
        return orderService.getUserOrders(user, pageable).map(OrderDto::from);
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public Page<OrderDto> getAllOrders(@PageableDefault(size = 20) Pageable pageable) {
        return orderService.getAllOrders(pageable).map(OrderDto::from);
    }

    @GetMapping("/{id}")
    public OrderDto getById(@PathVariable Long id, Authentication auth) {
        AppUser user = userService.findByUsername(auth.getName());
        BookOrder order = orderService.findById(id);

        boolean isAdmin = user.getRole().equals("ADMIN");
        boolean isOwner = order.getUser().getId().equals(user.getId());
        if (!isAdmin && !isOwner) {
            throw new com.ctbc.ebookstore.exception.ForbiddenException("Access denied");
        }

        return OrderDto.from(order);
    }

    /** 確認下單：建立 PENDING 訂單，不扣款 */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderDto createOrder(Authentication auth) {
        AppUser user = userService.findByUsername(auth.getName());
        return OrderDto.from(orderService.createFromCart(user));
    }

    /** 確認付款：扣款並完成訂單 */
    @PostMapping("/{id}/pay")
    public OrderDto confirmPayment(@PathVariable Long id, Authentication auth) {
        AppUser user = userService.findByUsername(auth.getName());
        return OrderDto.from(orderService.confirmPayment(id, user));
    }

    /** 使用者主動取消待付款訂單 */
    @PostMapping("/{id}/cancel")
    public OrderDto cancelOrder(@PathVariable Long id, Authentication auth) {
        AppUser user = userService.findByUsername(auth.getName());
        return OrderDto.from(orderService.cancelOrder(id, user));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public OrderDto updateStatus(@PathVariable Long id, @RequestBody OrderStatusRequest req) {
        return OrderDto.from(orderService.updateStatus(id, req.getStatus()));
    }

}
