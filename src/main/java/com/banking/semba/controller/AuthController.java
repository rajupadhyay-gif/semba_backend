package com.banking.semba.controller;

import com.banking.semba.dto.*;
import com.banking.semba.dto.response.BankMpinResponse;
import com.banking.semba.dto.response.BankOtpResponse;
import com.banking.semba.service.AuthService;
import com.banking.semba.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/semba/api")
public class AuthController {

    private final AuthService authService;
    private final CustomerService customerService;

    public AuthController(AuthService authService, CustomerService customerService) {
        this.authService = authService;
        this.customerService = customerService;
    }
    // ---------------- SIGNUP START ----------------
    @PostMapping("/signup")
    public Mono<ResponseEntity<ApiResponseDTO<BankOtpResponse>>> signupStart(
            @Valid @RequestBody SignupStartRequest req,
            @RequestHeader("X-IP") String ip,
            @RequestHeader("X-Device-Id") String deviceId,
            @RequestHeader(value = "X-Latitude", required = false) Double latitude,
            @RequestHeader(value = "X-Longitude", required = false) Double longitude) {

        req.setIp(ip);
        req.setDeviceId(deviceId);
        req.setLatitude(latitude);
        req.setLongitude(longitude);

        return authService.signupStart(req)
                .map(resp -> ResponseEntity.status(resp.getResponseCode()).body(resp));
    }

    // ---------------- VERIFY OTP ----------------
    @PostMapping("/signup/verify")
    public Mono<ResponseEntity<ApiResponseDTO<BankOtpResponse>>> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest req,
            @RequestHeader("X-IP") String ip,
            @RequestHeader("X-Device-Id") String deviceId,
            @RequestHeader(value = "X-Latitude", required = false) Double latitude,
            @RequestHeader(value = "X-Longitude", required = false) Double longitude) {

        req.setIp(ip);
        req.setDeviceId(deviceId);
        req.setLatitude(latitude);
        req.setLongitude(longitude);

        return authService.verifyOtp(req)
                .map(resp -> ResponseEntity.status(resp.getResponseCode()).body(resp));
    }

    // ---------------- SET MPIN ----------------
    @PostMapping("/mpin/set")
    public Mono<ResponseEntity<ApiResponseDTO<BankMpinResponse>>> setMpin(
            @Valid @RequestBody BankMpinRequest req,
            @RequestHeader("X-IP") String ip,
            @RequestHeader("X-Device-Id") String deviceId,
            @RequestHeader(value = "X-Latitude", required = false) Double latitude,
            @RequestHeader(value = "X-Longitude", required = false) Double longitude) {

        req.setIp(ip);
        req.setDeviceId(deviceId);
        req.setLatitude(latitude);
        req.setLongitude(longitude);

        return authService.setMpin(req)
                .map(resp -> ResponseEntity.status(resp.getResponseCode()).body(resp));
    }

    // ---------------- LOGIN ----------------
    @PostMapping("/login")
    public Mono<ResponseEntity<ApiResponseDTO<Map<String, Object>>>> login(
            @Valid @RequestBody LoginRequest req,
            @RequestHeader("X-IP") String ip,
            @RequestHeader("X-Device-Id") String deviceId,
            @RequestHeader(value = "X-Latitude", required = false) Double latitude,
            @RequestHeader(value = "X-Longitude", required = false) Double longitude) {

        req.setIp(ip);
        req.setDeviceId(deviceId);
        req.setLatitude(latitude);
        req.setLongitude(longitude);

        return authService.login(req)
                .map(resp -> ResponseEntity.status(resp.getResponseCode()).body(resp));
    }

    @GetMapping("/signupProfile")
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> getProfile(
            @RequestHeader("Authorization") String authHeader,
            @RequestHeader("X-Device-Id") String deviceId,
            @RequestHeader("X-IP") String ip,
            @RequestHeader(value = "X-Latitude", required = false) Double latitude,
            @RequestHeader(value = "X-Longitude", required = false) Double longitude
    ) {
        ApiResponseDTO<Map<String, Object>> response = customerService.getProfile(authHeader, ip, deviceId, latitude, longitude);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/signupProfile/{id}")
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> getAccountById(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader,
            @RequestHeader("X-Device-Id") String deviceId,
            @RequestHeader("X-IP") String ip,
            @RequestHeader(value = "X-Latitude", required = false) Double latitude,
            @RequestHeader(value = "X-Longitude", required = false) Double longitude
    ) {
        ApiResponseDTO<Map<String, Object>> response = customerService.getAccountById(id, authHeader, deviceId, ip, latitude, longitude);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}

