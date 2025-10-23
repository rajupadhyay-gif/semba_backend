package com.banking.semba.controller;

import com.banking.semba.dto.ApiResponseDTO;
import com.banking.semba.dto.CardOtpRequest;
import com.banking.semba.dto.CardRequest;
import com.banking.semba.dto.PayNowRequest;
import com.banking.semba.security.JwtTokenService;
import com.banking.semba.service.CardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/semba/api")
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;
    private final JwtTokenService jwtService;

    // ------------------- Add Card -------------------
    @PostMapping("/card/add")
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> addCard(
            @RequestHeader("Authorization") String authHeader,
            @RequestHeader("X-IP") String ip,
            @RequestHeader("X-Device-Id") String deviceId,
            @RequestHeader(value = "X-Latitude", required = false) Double latitude,
            @RequestHeader(value = "X-Longitude", required = false) Double longitude,
            @RequestBody CardRequest request
    ) {
        String mobile = jwtService.extractMobileFromHeader(authHeader);
        ApiResponseDTO<Map<String, Object>> response = cardService.addCard(
                request, mobile, ip, deviceId, latitude, longitude);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // ------------------- Verify Card OTP -------------------
    @PostMapping("card/verify-otp/")
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> verifyOtp(
            @RequestHeader("Authorization") String authHeader,
            @RequestHeader("X-IP") String ip,
            @RequestHeader("X-Device-Id") String deviceId,
            @RequestHeader(value = "X-Latitude", required = false) Double latitude,
            @RequestHeader(value = "X-Longitude", required = false) Double longitude,
            @RequestBody CardOtpRequest request
    ) {
        String mobile = jwtService.extractMobileFromHeader(authHeader);
        ApiResponseDTO<Map<String, Object>> response = cardService.verifyCardOtp(
                request, mobile, ip, deviceId, latitude, longitude);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // ------------------- Get All Cards -------------------
    @GetMapping("card/list")
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> getCards(
            @RequestHeader("Authorization") String authHeader,
            @RequestHeader("X-IP") String ip,
            @RequestHeader("X-Device-Id") String deviceId,
            @RequestHeader(value = "X-Latitude", required = false) Double latitude,
            @RequestHeader(value = "X-Longitude", required = false) Double longitude
    ) {
        String mobile = jwtService.extractMobileFromHeader(authHeader);
        ApiResponseDTO<Map<String, Object>> response = cardService.getCards(
                mobile, ip, deviceId, latitude, longitude);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
    @PostMapping("card/pay-now")
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> payNow(
            @RequestHeader("Authorization") String authHeader,
            @RequestHeader("X-IP") String ip,
            @RequestHeader("X-Device-Id") String deviceId,
            @RequestHeader(value = "X-Latitude", required = false) Double latitude,
            @RequestHeader(value = "X-Longitude", required = false) Double longitude,
            @Validated @RequestBody PayNowRequest request
    ) {
        String mobile = jwtService.extractMobileFromHeader(authHeader);
        ApiResponseDTO<Map<String, Object>> response = cardService.processPayment(request, mobile, ip, deviceId, latitude, longitude);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}

