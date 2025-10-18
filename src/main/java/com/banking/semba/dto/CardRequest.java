package com.banking.semba.dto;

import lombok.Data;

@Data
public class CardRequest {

    private String cardNumber;
    private String holderName;
    private String validThru; // MM/yy
}