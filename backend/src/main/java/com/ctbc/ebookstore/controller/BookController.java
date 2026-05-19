package com.ctbc.ebookstore.controller;

import com.ctbc.ebookstore.bean.AppUser;
import com.ctbc.ebookstore.dto.BookDto;
import com.ctbc.ebookstore.dto.BookRequest;
import com.ctbc.ebookstore.service.AppUserService;
import com.ctbc.ebookstore.service.BookService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

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
    public List<BookDto> getAll(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long sellerId) {

        if (categoryId != null) return bookService.findByCategory(categoryId).stream().map(BookDto::from).collect(Collectors.toList());
        if (sellerId   != null) return bookService.findBySeller(sellerId).stream().map(BookDto::from).collect(Collectors.toList());
        return bookService.findAll().stream().map(BookDto::from).collect(Collectors.toList());
    }

    /** Admin：取得所有書籍（含所有狀態） */
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public List<BookDto> getAllForAdmin() {
        return bookService.findAllForAdmin().stream().map(BookDto::from).collect(Collectors.toList());
    }

    /** 出版商：取得自己的所有書籍（含 draft/discontinued） */
    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public List<BookDto> getMyBooks(Authentication auth) {
        AppUser user = userService.findByUsername(auth.getName());
        return bookService.findMySells(user).stream().map(BookDto::from).collect(Collectors.toList());
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
