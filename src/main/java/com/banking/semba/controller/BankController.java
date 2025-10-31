package com.banking.semba.controller;

import com.banking.semba.constants.ValidationMessages;
import com.banking.semba.dto.ApiResponseDTO;
import com.banking.semba.dto.DownloadStatementDTO;
import com.banking.semba.dto.FundTransferDTO;
import com.banking.semba.security.JwtTokenService;
import com.banking.semba.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("semba/api/")
@RequiredArgsConstructor
public class BankController {

    private final AccountService accountService;
    private final JwtTokenService jwtService;

    /* Fetch Account Details */
    @GetMapping("/accounts/{id}")
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> getAccount(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader,
            @RequestHeader("X-IP") String ip,
            @RequestHeader("X-Device-Id") String deviceId,
            @RequestHeader(value = "X-Latitude", required = false) Double latitude,
            @RequestHeader(value = "X-Longitude", required = false) Double longitude) {

        String mobile = jwtService.extractMobileFromHeader(authHeader);

        ApiResponseDTO<Map<String, Object>> response =
                accountService.getAccountById(id, mobile, ip, deviceId, latitude, longitude);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /*Fetch Live Balance */
    @GetMapping("/accounts/{accountNumber}/balance")
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> getBalance(
            @PathVariable String accountNumber,
            @RequestHeader("Authorization") String authHeader,
            @RequestHeader("X-IP") String ip,
            @RequestHeader("X-Device-Id") String deviceId,
            @RequestHeader(value = "X-Latitude", required = false) Double latitude,
            @RequestHeader(value = "X-Longitude", required = false) Double longitude) {

        String mobile = jwtService.extractMobileFromHeader(authHeader);

        ApiResponseDTO<Map<String, Object>> response =
                accountService.getLiveBalance(accountNumber, mobile, ip, deviceId, latitude, longitude);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /* Fund Transfers (UPI, MOBILE, BANK, CREDIT/DEBIT CARD) */
    @PostMapping("/payments")
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> makePayment(
            @RequestBody FundTransferDTO dto,
            @RequestHeader("Authorization") String authHeader,
            @RequestHeader("X-IP") String ip,
            @RequestHeader("X-Device-Id") String deviceId,
            @RequestHeader(value = "X-Latitude", required = false) Double latitude,
            @RequestHeader(value = "X-Longitude", required = false) Double longitude) {

        String mobile = jwtService.extractMobileFromHeader(authHeader);

        ApiResponseDTO<Map<String, Object>> response =
                accountService.transferFunds(dto, mobile, ip, deviceId, latitude, longitude);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/account-statement")
    public ResponseEntity<?> downloadStatement(
            @RequestHeader("Authorization") String auth,
            @RequestHeader("X-IP") String ip,
            @RequestHeader("X-Device-Id") String deviceId,
            @RequestHeader(value = "X-Latitude", required = false) Double latitude,
            @RequestHeader(value = "X-Longitude", required = false) Double longitude,
            @RequestParam String accountNumber,
            @RequestParam(defaultValue = "pdf") String format,
            @RequestParam(defaultValue = "30") String range) {

        String mobile = jwtService.extractMobileFromHeader(auth);
        if (mobile == null || mobile.isEmpty()) {
            ApiResponseDTO<DownloadStatementDTO> unauthorizedResponse = new ApiResponseDTO<>(
                    ValidationMessages.STATUS_UNAUTHORIZED,
                    HttpStatus.UNAUTHORIZED.value(),
                    ValidationMessages.INVALID_JWT,
                    null
            );
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(unauthorizedResponse);
        }

        if (format.equalsIgnoreCase("pdf") || format.equalsIgnoreCase("xlsx")) {
            return accountService.downloadStatement(auth, ip, deviceId, latitude, longitude, accountNumber, range, format);
        }

        ApiResponseDTO<DownloadStatementDTO> invalidFormat = new ApiResponseDTO<>(
                ValidationMessages.BAD_REQUEST,
                HttpStatus.BAD_REQUEST.value(),
                "Invalid format. Supported formats: pdf, xlsx",
                null
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(invalidFormat);
    }

//    @GetMapping("/filter")
//    public ResponseEntity<?> filterStatement(
//            @RequestHeader("Authorization") String auth,
//            @RequestHeader("X-IP") String ip,
//            @RequestHeader("X-Device-Id") String deviceId,
//            @RequestParam String accountNumber,
//            @RequestParam(required = false) String range,          // e.g. "30days", "90days", "180days", "365days"
//            @RequestParam(required = false) String fromDate,       // custom start date yyyy-MM-dd
//            @RequestParam(required = false) String toDate,         // custom end date yyyy-MM-dd
//            @RequestParam(required = false) String financialYear,  // e.g. "2024-2025"
//            @RequestParam(defaultValue = "json") String format     // pdf, excel, json
//    ) {
//        return accountService.getFilteredTransactions(
//                auth, ip, deviceId, accountNumber, range, fromDate, toDate, financialYear, format);
//    }
}