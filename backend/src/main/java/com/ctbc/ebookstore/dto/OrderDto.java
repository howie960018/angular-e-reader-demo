package com.ctbc.ebookstore.dto;

import com.ctbc.ebookstore.bean.BookOrder;
import com.ctbc.ebookstore.bean.OrderItem;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter @Setter
public class OrderDto {
    private Long id;
    private Long userId;
    private String username;
    private List<OrderItemDto> items;
    private BigDecimal totalPrice;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime expiresAt;

    @Getter @Setter
    public static class OrderItemDto {
        private Long id;
        private Long orderId;
        private Long bookId;
        private BookDto book;
        private BigDecimal price;
        private int quantity;

        public static OrderItemDto from(OrderItem item) {
            OrderItemDto dto = new OrderItemDto();
            dto.setId(item.getId());
            dto.setOrderId(item.getOrder().getId());
            dto.setBookId(item.getBook().getId());
            dto.setBook(BookDto.from(item.getBook()));
            dto.setPrice(item.getPrice());
            dto.setQuantity(item.getQuantity());
            return dto;
        }
    }

    public static OrderDto from(BookOrder order) {
        OrderDto dto = new OrderDto();
        dto.setId(order.getId());
        dto.setUserId(order.getUser().getId());
        dto.setUsername(order.getUser().getUsername());
        dto.setTotalPrice(order.getTotalPrice());
        dto.setStatus(order.getStatus());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setUpdatedAt(order.getUpdatedAt());
        dto.setExpiresAt(order.getExpiresAt());

        List<OrderItemDto> itemDtos = order.getItems().stream()
                .map(OrderItemDto::from)
                .collect(Collectors.toList());
        dto.setItems(itemDtos);

        return dto;
    }
}
