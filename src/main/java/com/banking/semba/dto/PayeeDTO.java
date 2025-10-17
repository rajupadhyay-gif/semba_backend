package com.banking.semba.dto;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayeeDTO {
    private String beneficiaryName;
    private String beneficiaryAccountNumber;
    private String ifscCode;
    private String bankId;
    private String beneficiaryMobileNumber;
}

