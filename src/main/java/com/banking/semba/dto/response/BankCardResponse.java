package com.banking.semba.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BankCardResponse {

    private boolean success;
    private String message;
    private CardDetail card;
    private List<CardDetail> cards;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CardDetail {
        private String cardNumber;
        private String holderName;
        private String validThru;
        private boolean verified;
        private LocalDateTime otpSentAt;
        private LocalDateTime addedAt;
        private Map<String, Object> metadata;
    }
}
