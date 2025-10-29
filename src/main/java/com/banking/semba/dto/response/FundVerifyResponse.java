package com.banking.semba.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FundVerifyResponse {

    private String transactionId;
    private String toName;
    private String toAccountNumber;
    private String fromName;
    private String fromAccountNumber;
    private String paymentMode; // e.g., "NEFT"
    private Double amount;
    private String remark;
    private LocalDateTime scheduledDateTime; // combine date + time
    private LocalDate scheduledDate;
    }

