package com.banking.semba.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ApiResponses<T> {
    private String status;
    private int code;
    private String message;
    private T data;
}