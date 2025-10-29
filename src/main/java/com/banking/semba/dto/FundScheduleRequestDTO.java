package com.banking.semba.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class FundScheduleRequestDTO {
    private String fromAccountNumber;
    private String toAccountNumber;
    private BigDecimal amount;
    private String transferType; // IMPS / NEFT / RTGS
    private LocalDate scheduledDate;
    private LocalTime scheduledTime;
    private String remark;
}