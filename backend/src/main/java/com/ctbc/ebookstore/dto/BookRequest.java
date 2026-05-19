package com.ctbc.ebookstore.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter
public class BookRequest {
    @NotBlank
    private String title;

    @NotBlank
    private String author;

    private String description;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal price;

    private Long categoryId;

    private String coverImage;

    private String content;

    /** draft | active | discontinued | banned (admin only) */
    private String status = "active";
}
