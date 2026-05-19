package com.ctbc.ebookstore.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CartItemRequest {
    @NotNull
    private Long bookId;

    @Min(1)
    private int quantity = 1;
}
