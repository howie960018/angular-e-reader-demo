package com.ctbc.ebookstore.controller;

import com.ctbc.ebookstore.bean.AppUser;
import com.ctbc.ebookstore.dto.BookDto;
import com.ctbc.ebookstore.dto.BookRequest;
import com.ctbc.ebookstore.service.AppUserService;
import com.ctbc.ebookstore.service.BookService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/books")
public class BookController {

    private final BookService bookService;
    private final AppUserService userService;

    public BookController(BookService bookService, AppUserService userService) {
        this.bookService = bookService;
        this.userService = userService;
    }

    /** 公開：只回傳 active 書籍 */
    @GetMapping
    public Page<BookDto> getAll(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long sellerId,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20) Pageable pageable) {

        boolean hasKeyword = keyword != null && !keyword.isBlank();
        if (hasKeyword && categoryId != null) return bookService.searchByCategory(categoryId, keyword, pageable).map(BookDto::from);
        if (hasKeyword) return bookService.search(keyword, pageable).map(BookDto::from);
        if (categoryId != null) return bookService.findByCategory(categoryId, pageable).map(BookDto::from);
        if (sellerId   != null) return bookService.findBySeller(sellerId, pageable).map(BookDto::from);
        return bookService.findAll(pageable).map(BookDto::from);
    }

    /** Admin：取得所有書籍（含所有狀態） */
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public Page<BookDto> getAllForAdmin(@PageableDefault(size = 20) Pageable pageable) {
        return bookService.findAllForAdmin(pageable).map(BookDto::from);
    }

    /** 出版商：取得自己的所有書籍（含 draft/discontinued） */
    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public Page<BookDto> getMyBooks(Authentication auth, @PageableDefault(size = 20) Pageable pageable) {
        AppUser user = userService.findByUsername(auth.getName());
        return bookService.findMySells(user, pageable).map(BookDto::from);
    }

    @GetMapping("/{id}")
    public BookDto getById(@PathVariable Long id) {
        return BookDto.from(bookService.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public BookDto create(@Valid @RequestBody BookRequest req, Authentication auth) {
        AppUser user = userService.findByUsername(auth.getName());
        return BookDto.from(bookService.create(req, user));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public BookDto update(@PathVariable Long id, @Valid @RequestBody BookRequest req, Authentication auth) {
        AppUser user = userService.findByUsername(auth.getName());
        return BookDto.from(bookService.update(id, req, user));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public void delete(@PathVariable Long id, Authentication auth) {
        AppUser user = userService.findByUsername(auth.getName());
        bookService.delete(id, user);
    }
}
