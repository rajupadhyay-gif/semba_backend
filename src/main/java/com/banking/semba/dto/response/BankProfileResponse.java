package com.banking.semba.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BankProfileResponse {
    private boolean success;
    private Map<String, Object> profile;
}