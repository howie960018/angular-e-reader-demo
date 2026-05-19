package com.ctbc.ebookstore.controller;

import com.ctbc.ebookstore.bean.AppUser;
import com.ctbc.ebookstore.bean.Cart;
import com.ctbc.ebookstore.dto.CartDto;
import com.ctbc.ebookstore.dto.CartItemRequest;
import com.ctbc.ebookstore.dto.UpdateQuantityRequest;
import com.ctbc.ebookstore.service.AppUserService;
import com.ctbc.ebookstore.service.CartService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;
    private final AppUserService userService;

    public CartController(CartService cartService, AppUserService userService) {
        this.cartService = cartService;
        this.userService = userService;
    }

    @GetMapping
    public CartDto getCart(Authentication auth) {
        AppUser user = userService.findByUsername(auth.getName());
        Cart cart = cartService.getOrCreateCart(user);
        return CartDto.from(cart);
    }

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    public CartDto addItem(@Valid @RequestBody CartItemRequest req, Authentication auth) {
        AppUser user = userService.findByUsername(auth.getName());
        Cart cart = cartService.addItem(user, req.getBookId(), req.getQuantity());
        return CartDto.from(cart);
    }

    @PutMapping("/items/{bookId}")
    public CartDto updateQuantity(@PathVariable Long bookId,
                                  @RequestBody UpdateQuantityRequest req,
                                  Authentication auth) {
        AppUser user = userService.findByUsername(auth.getName());
        Cart cart = cartService.updateItemQuantity(user, bookId, req.getQuantity());
        return CartDto.from(cart);
    }

    @DeleteMapping("/items/{bookId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeItem(@PathVariable Long bookId, Authentication auth) {
        AppUser user = userService.findByUsername(auth.getName());
        cartService.removeItem(user, bookId);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearCart(Authentication auth) {
        AppUser user = userService.findByUsername(auth.getName());
        cartService.clearCart(user);
    }
}
