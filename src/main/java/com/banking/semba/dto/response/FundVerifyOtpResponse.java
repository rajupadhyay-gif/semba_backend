package com.banking.semba.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FundVerifyOtpResponse {
    private String transactionId;
    private boolean success;
    private String message;
    private LocalDateTime completedAt;

    private String toName;
    private String toAccount;
    private String fromName;
    private String fromAccount;

    private String transferType; // IMPS, NEFT, RTGS
    private BigDecimal amount;
    private String remark;
}