package com.banking.semba.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class FundTransferRequestDTO {

    private String fromAccountNumber;
    private String toAccountNumber;
    private BigDecimal amount;
    private String transferType; // IMPS, NEFT, RTGS
    private String remark;
}