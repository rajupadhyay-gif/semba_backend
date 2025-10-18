package com.banking.semba.dto.response;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class BankCardResponse {

    private boolean success;
    private String message;
    private Map<String, Object> card;
    private List<Map<String, Object>> cards;
}
