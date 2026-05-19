package com.ctbc.ebookstore.dto;

import com.ctbc.ebookstore.bean.Book;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter
public class BookDto {
    private Long id;
    private String title;
    private String author;
    private String description;
    private BigDecimal price;
    private Long categoryId;
    private String categoryName;
    private Long sellerId;
    private String sellerName;
    private String coverImage;
    private String content;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static BookDto from(Book book) {
        BookDto dto = new BookDto();
        dto.setId(book.getId());
        dto.setTitle(book.getTitle());
        dto.setAuthor(book.getAuthor());
        dto.setDescription(book.getDescription());
        dto.setPrice(book.getPrice());
        if (book.getCategory() != null) {
            dto.setCategoryId(book.getCategory().getId());
            dto.setCategoryName(book.getCategory().getName());
        }
        dto.setSellerId(book.getSeller().getId());
        dto.setSellerName(book.getSeller().getUsername());
        dto.setCoverImage(book.getCoverImage());
        dto.setContent(book.getContent());
        dto.setStatus(book.getStatus());
        dto.setCreatedAt(book.getCreatedAt());
        dto.setUpdatedAt(book.getUpdatedAt());
        return dto;
    }
}
