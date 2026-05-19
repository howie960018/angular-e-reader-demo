package com.ctbc.ebookstore.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter
public class CreateTopUpCodeRequest {
    @NotBlank
    private String code;

    @NotNull
    @DecimalMin("1.00")
    private BigDecimal amount;
}
