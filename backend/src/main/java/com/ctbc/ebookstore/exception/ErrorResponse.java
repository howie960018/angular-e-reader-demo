package com.ctbc.ebookstore.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter @Setter @AllArgsConstructor
public class ErrorResponse {
    private Date timestamp;
    private String message;
    private String path;
}
