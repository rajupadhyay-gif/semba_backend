package com.banking.semba.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FundTransferDTO {
    private String fromAccount;      // Sender account number
    private String toAccount;        // Receiver account / mobile / UPI
    private BigDecimal amount;
    private String transactionId;    // Idempotency
    private PaymentType paymentType; // ENUM
}