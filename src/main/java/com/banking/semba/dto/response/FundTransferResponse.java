package com.banking.semba.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data

public class FundTransferResponse {

    private String fromAccount;
    private BigDecimal amount;
    private String currency;
    private String transactionId;
    private String transferType;
    private LocalDateTime createdAt;
    public FundTransferResponse(String fromAccount, BigDecimal amount, String currency) {
        this.fromAccount = fromAccount;
        this.amount = amount;
        this.currency = currency;
    }
}