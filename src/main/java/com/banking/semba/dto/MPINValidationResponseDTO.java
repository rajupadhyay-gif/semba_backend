package com.banking.semba.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class MPINValidationResponseDTO {
    private boolean valid;
    private String message;
    private String transactionId;

}
