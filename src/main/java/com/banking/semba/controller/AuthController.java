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

    @PostMapping("/signup")
    public Mono<ResponseEntity<ApiResponses<BankOtpResponse>>> signupStart(@Valid @RequestBody SignupStartRequest req) {
        return authService.signupStart(req)
                .map(response -> ResponseEntity.status(HttpStatus.OK).body(response));

    }

    @PostMapping("/signupVerify")
    public Mono<ResponseEntity<ApiResponses<BankOtpResponse>>> verifyOtp(
            @RequestBody @Valid VerifyOtpRequest req) {
        return authService.verifyOtp(req)
                .map(resp -> ResponseEntity.status(HttpStatus.OK).body(resp));
    }

    @PostMapping("/setMpin")
    public Mono<ResponseEntity<ApiResponses<BankMpinResponse>>> setMpin(@RequestBody @Valid BankMpinRequest req) {
        return authService.setMpin(req)
                .map(resp -> ResponseEntity.status(HttpStatus.OK).body(resp));
    }

    @PostMapping("/login")
    public Mono<ResponseEntity<ApiResponses<Map<String, Object>>>> login(@RequestBody @Valid LoginRequest req) {
        return authService.login(req)
                .map(resp -> ResponseEntity.status(HttpStatus.OK).body(resp));
    }
    @GetMapping("/signupProfile")
    public ResponseEntity<ApiResponses<Map<String, Object>>> getProfile(
            @RequestHeader("Authorization") String authHeader,
            @RequestHeader("X-Device-Id") String deviceId,
            @RequestHeader("X-IP") String ip,
            @RequestHeader(value = "X-Latitude", required = false) Double latitude,
            @RequestHeader(value = "X-Longitude", required = false) Double longitude
    ) {
        ApiResponses<Map<String, Object>> response = customerService.getProfile(authHeader, ip, deviceId, latitude, longitude);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/signupProfile/{id}")
    public ResponseEntity<ApiResponses<Map<String, Object>>> getAccountById(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader,
            @RequestHeader("X-Device-Id") String deviceId,
            @RequestHeader("X-IP") String ip,
            @RequestHeader(value = "X-Latitude", required = false) Double latitude,
            @RequestHeader(value = "X-Longitude", required = false) Double longitude
    ) {
        ApiResponses<Map<String, Object>> response = customerService.getAccountById(id, authHeader, deviceId, ip, latitude, longitude);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}

