package com.ctbc.ebookstore.controller;

import com.ctbc.ebookstore.bean.AppUser;
import com.ctbc.ebookstore.bean.Book;
import com.ctbc.ebookstore.dto.BookContentResponse;
import com.ctbc.ebookstore.dto.BookDto;
import com.ctbc.ebookstore.exception.ForbiddenException;
import com.ctbc.ebookstore.repository.OrderItemRepository;
import com.ctbc.ebookstore.service.AppUserService;
import com.ctbc.ebookstore.service.BookService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/books")
public class BookContentController {

    private static final int PREVIEW_LENGTH = 1500;

    private final BookService bookService;
    private final AppUserService userService;
    private final OrderItemRepository orderItemRepo;

    public BookContentController(BookService bookService,
                                 AppUserService userService,
                                 OrderItemRepository orderItemRepo) {
        this.bookService = bookService;
        this.userService = userService;
        this.orderItemRepo = orderItemRepo;
    }

    /**
     * 書籍內容存取：
     * - banned 書籍 → 403（法律問題，即使已購買也無法閱讀）
     * - 未登入 / 未購買 → 前 1500 字試閱
     * - 已購買 / 出版商本人 / Admin → 完整內容
     */
    @GetMapping("/{id}/content")
    public BookContentResponse getContent(@PathVariable Long id, Authentication auth) {
        Book book = bookService.findById(id);

        if ("banned".equals(book.getStatus())) {
            throw new ForbiddenException("此書已依法強制下架，無法閱讀");
        }

        String full = book.getContent() != null ? book.getContent() : "（此書暫無內容）";
        boolean hasAccess = false;

        if (auth != null) {
            AppUser user = userService.findByUsername(auth.getName());
            boolean isAdmin   = "ADMIN".equals(user.getRole());
            boolean isOwner   = book.getSeller().getId().equals(user.getId());
            boolean purchased = !isAdmin && !isOwner && orderItemRepo.hasPurchasedBook(user, book);
            hasAccess = isAdmin || isOwner || purchased;
        }

        String content = hasAccess
                ? full
                : (full.length() > PREVIEW_LENGTH ? full.substring(0, PREVIEW_LENGTH) : full);

        return new BookContentResponse(content, hasAccess, book.getTitle(), PREVIEW_LENGTH, full.length());
    }

    /**
     * 確認目前登入用戶是否已購買此書（純粹購買記錄，不含 seller/admin 免費存取）
     */
    @GetMapping("/{id}/purchased")
    public Map<String, Boolean> checkPurchased(@PathVariable Long id, Authentication auth) {
        if (auth == null) return Map.of("purchased", false);
        AppUser user = userService.findByUsername(auth.getName());
        Book book = bookService.findById(id);
        boolean purchased = orderItemRepo.hasPurchasedBook(user, book);
        return Map.of("purchased", purchased);
    }

    /**
     * 已購買書籍列表（「我的書籍」頁面）
     */
    @GetMapping("/purchased")
    public List<BookDto> getPurchasedBooks(Authentication auth) {
        AppUser user = userService.findByUsername(auth.getName());
        return orderItemRepo.findPurchasedBooksByUser(user)
                .stream()
                .map(BookDto::from)
                .collect(Collectors.toList());
    }
}
