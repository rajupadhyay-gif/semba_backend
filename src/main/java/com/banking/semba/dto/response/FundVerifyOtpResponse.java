package com.banking.semba.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FundVerifyOtpResponse {

    private String transactionId;
    private boolean success;
    private String message;
    private LocalDateTime completedAt;
    private String toName;
    private String toAccount;
    private String fromName;
    private String fromAccount;;
}