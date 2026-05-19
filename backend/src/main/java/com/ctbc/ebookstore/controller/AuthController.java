package com.ctbc.ebookstore.controller;

import com.ctbc.ebookstore.bean.AppUser;
import com.ctbc.ebookstore.bean.Cart;
import com.ctbc.ebookstore.bean.Wallet;
import com.ctbc.ebookstore.dto.AuthRequest;
import com.ctbc.ebookstore.dto.AuthResponse;
import com.ctbc.ebookstore.dto.RegisterRequest;
import com.ctbc.ebookstore.dto.UserDto;
import com.ctbc.ebookstore.repository.CartRepository;
import com.ctbc.ebookstore.repository.WalletRepository;
import com.ctbc.ebookstore.security.JwtService;
import com.ctbc.ebookstore.service.AppUserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authManager;
    private final AppUserService userService;
    private final JwtService jwtService;
    private final CartRepository cartRepo;
    private final WalletRepository walletRepo;

    public AuthController(AuthenticationManager authManager,
                          AppUserService userService,
                          JwtService jwtService,
                          CartRepository cartRepo,
                          WalletRepository walletRepo) {
        this.authManager = authManager;
        this.userService = userService;
        this.jwtService = jwtService;
        this.cartRepo = cartRepo;
        this.walletRepo = walletRepo;
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody AuthRequest req) {
        Authentication auth;
        try {
            auth = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword()));
        } catch (AuthenticationException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        UserDetails userDetails = userService.loadUserByUsername(auth.getName());
        String token = jwtService.generateToken(userDetails);
        AppUser user = userService.findByUsername(auth.getName());

        return new AuthResponse(token, UserDto.from(user));
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest req) {
        if (userService.existsByUsername(req.getUsername())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }
        if (userService.existsByEmail(req.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }

        String role = req.getRole() != null ? req.getRole().toUpperCase() : "USER";
        AppUser user = userService.registerUser(req.getUsername(), req.getEmail(), req.getPassword(), role);

        // Create cart for new user
        cartRepo.findByUser(user).orElseGet(() -> cartRepo.save(new Cart(user)));

        // Create wallet for new user
        walletRepo.findByUser(user).orElseGet(() -> {
            String walletType = role.equals("SELLER") ? "seller" : "user";
            return walletRepo.save(new Wallet(user, walletType, new BigDecimal("1000")));
        });

        UserDetails userDetails = userService.loadUserByUsername(user.getUsername());
        String token = jwtService.generateToken(userDetails);

        return new AuthResponse(token, UserDto.from(user));
    }

    @GetMapping("/me")
    public UserDto getCurrentUser(Authentication auth) {
        if (auth == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        AppUser user = userService.findByUsername(auth.getName());
        return UserDto.from(user);
    }
}
