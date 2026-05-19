package com.ctbc.ebookstore.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class TopUpCodeRequest {
    @NotBlank
    private String code;
}
