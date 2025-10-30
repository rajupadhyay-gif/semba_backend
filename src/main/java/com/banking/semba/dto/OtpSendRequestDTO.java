package com.banking.semba.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpSendRequestDTO {


    private String mobile;
    private String context;
    private String referenceId;
}