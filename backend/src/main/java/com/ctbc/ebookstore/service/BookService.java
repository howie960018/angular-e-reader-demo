package com.ctbc.ebookstore.service;

import com.ctbc.ebookstore.bean.AppUser;
import com.ctbc.ebookstore.bean.Book;
import com.ctbc.ebookstore.bean.Category;
import com.ctbc.ebookstore.dto.BookRequest;
import com.ctbc.ebookstore.exception.ForbiddenException;
import com.ctbc.ebookstore.exception.ResourceNotFoundException;
import com.ctbc.ebookstore.repository.BookRepository;
import com.ctbc.ebookstore.repository.CategoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BookService {

    private final BookRepository bookRepo;
    private final CategoryRepository categoryRepo;

    public BookService(BookRepository bookRepo, CategoryRepository categoryRepo) {
        this.bookRepo = bookRepo;
        this.categoryRepo = categoryRepo;
    }

    /** 公開首頁：只回傳 active 書籍 */
    public List<Book> findAll() {
        return bookRepo.findByStatus("active");
    }

    public Page<Book> findAll(Pageable pageable) {
        return bookRepo.findByStatus("active", pageable);
    }

    /** Admin 後台：回傳所有書籍（含所有狀態） */
    public List<Book> findAllForAdmin() {
        return bookRepo.findAll();
    }

    public Page<Book> findAllForAdmin(Pageable pageable) {
        return bookRepo.findAll(pageable);
    }

    /** 出版商後台：回傳自己所有書籍（含 draft/discontinued） */
    public List<Book> findMySells(AppUser seller) {
        return bookRepo.findBySellerId(seller.getId());
    }

    public Page<Book> findMySells(AppUser seller, Pageable pageable) {
        return bookRepo.findBySellerId(seller.getId(), pageable);
    }

    public Book findById(Long id) {
        return bookRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found: " + id));
    }

    public List<Book> findByCategory(Long categoryId) {
        return bookRepo.findByCategoryIdAndStatus(categoryId, "active");
    }

    public Page<Book> findByCategory(Long categoryId, Pageable pageable) {
        return bookRepo.findByCategoryIdAndStatus(categoryId, "active", pageable);
    }

    public Page<Book> search(String keyword, Pageable pageable) {
        return bookRepo.searchByKeyword(keyword, pageable);
    }

    public Page<Book> searchByCategory(Long categoryId, String keyword, Pageable pageable) {
        return bookRepo.searchByCategoryAndKeyword(categoryId, keyword, pageable);
    }

    public List<Book> findBySeller(Long sellerId) {
        return bookRepo.findBySellerIdAndStatus(sellerId, "active");
    }

    public Page<Book> findBySeller(Long sellerId, Pageable pageable) {
        return bookRepo.findBySellerIdAndStatus(sellerId, "active", pageable);
    }

    public Book create(BookRequest req, AppUser seller) {
        Book book = new Book();
        book.setTitle(req.getTitle());
        book.setAuthor(req.getAuthor());
        book.setDescription(req.getDescription());
        book.setPrice(req.getPrice());
        book.setSeller(seller);
        book.setCoverImage(req.getCoverImage());
        book.setContent(req.getContent());
        book.setStatus(req.getStatus() != null ? req.getStatus() : "active");

        if (req.getCategoryId() != null) {
            Category category = categoryRepo.findById(req.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + req.getCategoryId()));
            book.setCategory(category);
        }
        return bookRepo.save(book);
    }

    public Book update(Long id, BookRequest req, AppUser currentUser) {
        Book book = findById(id);
        boolean isAdmin = "ADMIN".equals(currentUser.getRole());
        boolean isOwner = book.getSeller().getId().equals(currentUser.getId());
        if (!isAdmin && !isOwner) {
            throw new ForbiddenException("You do not have permission to update this book");
        }

        book.setTitle(req.getTitle());
        book.setAuthor(req.getAuthor());
        book.setDescription(req.getDescription());
        book.setPrice(req.getPrice());
        book.setCoverImage(req.getCoverImage());
        book.setContent(req.getContent());
        book.setUpdatedAt(LocalDateTime.now());

        if (req.getStatus() != null) {
            if ("banned".equals(req.getStatus()) && !isAdmin) {
                throw new ForbiddenException("Only admin can ban a book");
            }
            book.setStatus(req.getStatus());
        }

        if (req.getCategoryId() != null) {
            Category category = categoryRepo.findById(req.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + req.getCategoryId()));
            book.setCategory(category);
        }
        return bookRepo.save(book);
    }

    public void delete(Long id, AppUser currentUser) {
        Book book = findById(id);
        boolean isAdmin = "ADMIN".equals(currentUser.getRole());
        boolean isOwner = book.getSeller().getId().equals(currentUser.getId());
        if (!isAdmin && !isOwner) {
            throw new ForbiddenException("You do not have permission to delete this book");
        }
        bookRepo.delete(book);
    }
}
