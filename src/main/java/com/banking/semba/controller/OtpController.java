package com.banking.semba.controller;

import com.banking.semba.dto.HttpResponseDTO;
import com.banking.semba.dto.OtpSendRequestDTO;
import com.banking.semba.service.OtpService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("semba/api")
@RequiredArgsConstructor
public class OtpController {

    private final OtpService otpService;

    //Send OTP (login, transfer, etc.)
    @PostMapping("/send")
    public ResponseEntity<HttpResponseDTO> sendOtp(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestHeader("X-IP") String ip,
            @RequestHeader("X-Device-Id") String deviceId,
            @RequestHeader(value = "X-Latitude", required = false) Double latitude,
            @RequestHeader(value = "X-Longitude", required = false) Double longitude,
            @RequestParam boolean preLogin, // true = login otp, false = others
            @RequestBody OtpSendRequestDTO dto) {

        return ResponseEntity.ok(
                otpService.sendOtp(auth, ip, deviceId, latitude, longitude, dto, preLogin)
        );
    }
}
