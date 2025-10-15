package com.banking.semba.controller;

import com.banking.semba.dto.ApiResponses;
import com.banking.semba.dto.SignupStartRequest;
import com.banking.semba.dto.VerifyOtpRequest;
import com.banking.semba.dto.response.BankOtpResponse;
import com.banking.semba.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/semba/api")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
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
}