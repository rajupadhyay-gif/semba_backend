package com.banking.semba.controller;

import com.banking.semba.dto.FundScheduleRequestDTO;
import com.banking.semba.dto.HttpResponseDTO;
import com.banking.semba.dto.OtpVerifyRequestDTO;
import com.banking.semba.security.JwtTokenService;
import com.banking.semba.service.FundSchedulerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/semba/api")
@RequiredArgsConstructor
public class FundSchedulerController {

    private final FundSchedulerService fundSchedulerService;
    private final JwtTokenService jwtTokenService;
    // ---------------- SCHEDULE PAYMENT ----------------
    @PostMapping("/schedule")
    public ResponseEntity<HttpResponseDTO> scheduleTransfer(
            @RequestHeader("Authorization") String auth,
            @RequestHeader("X-IP") String ip,
            @RequestHeader("X-Device-Id") String deviceId,
            @RequestHeader(value = "X-Latitude", required = false) Double latitude,
            @RequestHeader(value = "X-Longitude", required = false) Double longitude,
            @Valid @RequestBody FundScheduleRequestDTO request) {

        String mobile = jwtTokenService.extractMobileFromHeader(auth);
        HttpResponseDTO response = fundSchedulerService.scheduleTransfer(mobile, ip, deviceId, latitude, longitude, request);
        return ResponseEntity.status(response.getResponseCode()).body(response);
    }

    // ---------------- VERIFY OTP ----------------
    @PostMapping("/verify-otp")
    public ResponseEntity<HttpResponseDTO> verifyOtp(
            @RequestHeader("Authorization") String auth,
            @RequestHeader("X-IP") String ip,
            @RequestHeader("X-Device-Id") String deviceId,
            @RequestHeader(value = "X-Latitude", required = false) Double latitude,
            @RequestHeader(value = "X-Longitude", required = false) Double longitude,
            @Valid @RequestBody OtpVerifyRequestDTO otpRequest) {

        String mobile = jwtTokenService.extractMobileFromHeader(auth);
        HttpResponseDTO response = fundSchedulerService.verifyOtp(mobile, ip, deviceId, latitude, longitude, otpRequest);
        return ResponseEntity.status(response.getResponseCode()).body(response);
    }
}