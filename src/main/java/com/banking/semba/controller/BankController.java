package com.banking.semba.controller;

import com.banking.semba.dto.ApiResponseDTO;
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
}