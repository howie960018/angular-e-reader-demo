package com.ctbc.ebookstore.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter @AllArgsConstructor
public class BookContentResponse {
    private String content;
    private boolean hasAccess;
    private String bookTitle;
    private int previewLength;
    private int totalLength;
}
