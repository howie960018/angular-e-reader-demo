package com.ctbc.ebookstore.dto;

import com.ctbc.ebookstore.bean.Cart;
import com.ctbc.ebookstore.bean.CartItem;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter @Setter
public class CartDto {
    private Long id;
    private Long userId;
    private List<CartItemDto> items;
    private BigDecimal totalPrice;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Getter @Setter
    public static class CartItemDto {
        private Long id;
        private Long bookId;
        private BookDto book;
        private int quantity;

        public static CartItemDto from(CartItem item) {
            CartItemDto dto = new CartItemDto();
            dto.setId(item.getId());
            dto.setBookId(item.getBook().getId());
            dto.setBook(BookDto.from(item.getBook()));
            dto.setQuantity(item.getQuantity());
            return dto;
        }
    }

    public static CartDto from(Cart cart) {
        CartDto dto = new CartDto();
        dto.setId(cart.getId());
        dto.setUserId(cart.getUser().getId());
        dto.setCreatedAt(cart.getCreatedAt());
        dto.setUpdatedAt(cart.getUpdatedAt());

        List<CartItemDto> itemDtos = cart.getItems().stream()
                .map(CartItemDto::from)
                .collect(Collectors.toList());
        dto.setItems(itemDtos);

        BigDecimal total = itemDtos.stream()
                .map(i -> i.getBook().getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        dto.setTotalPrice(total);

        return dto;
    }
}
