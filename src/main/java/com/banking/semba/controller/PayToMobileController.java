package com.banking.semba.controller;


import com.banking.semba.dto.ApiResponseDTO;
import com.banking.semba.dto.RecentPaymentsDTO;
import com.banking.semba.security.JwtTokenService;
import com.banking.semba.service.PayToMobileService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/semba/api/pay/mobile")
public class PayToMobileController {

    private final PayToMobileService payToMobileService;
    private final JwtTokenService jwtTokenService;

    public PayToMobileController(PayToMobileService payToMobileService, JwtTokenService jwtTokenService) {
        this.payToMobileService = payToMobileService;
        this.jwtTokenService = jwtTokenService;
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponseDTO<List<Map<String, Object>>>> searchContacts(
            @RequestHeader("Authorization") String auth,
            @RequestHeader("X-IP") String ip,
            @RequestHeader("X-Device-Id") String deviceId,
            @RequestHeader(value = "X-Latitude", required = false) Double latitude,
            @RequestHeader(value = "X-Longitude", required = false) Double longitude,
            @RequestParam(value = "mobile", required = false) String mobileNumber,
            @RequestParam(value = "name", required = false) String name
    ) {
        ApiResponseDTO<List<Map<String, Object>>> contactResponseDTO = new ApiResponseDTO<>();
        String mobile = jwtTokenService.extractMobileFromHeader(auth);
        if (mobile == null || mobile.isEmpty()) {
            return new ResponseEntity<>(contactResponseDTO, HttpStatus.UNAUTHORIZED);
        }

        ApiResponseDTO<List<Map<String, Object>>> response = payToMobileService.searchContacts(
                auth, ip, deviceId, latitude, longitude, mobileNumber, name
        );
        return ResponseEntity.status(response.getResponseCode()).body(response);
    }


    @GetMapping("/recent/payments")
    public ResponseEntity<ApiResponseDTO<List<RecentPaymentsDTO>>> getRecentPayments(
            @RequestHeader("Authorization") String auth,
            @RequestHeader("X-IP") String ip,
            @RequestHeader("X-Device-Id") String deviceId,
            @RequestHeader(value = "X-Latitude", required = false) Double latitude,
            @RequestHeader(value = "X-Longitude", required = false) Double longitude
    ) {
        ApiResponseDTO<List<RecentPaymentsDTO>> apiResponseDTO = new ApiResponseDTO<>();

        String mobile = jwtTokenService.extractMobileFromHeader(auth);
        if (mobile == null || mobile.isEmpty()) {
            return new ResponseEntity<>(apiResponseDTO, HttpStatus.UNAUTHORIZED);
        }

        ApiResponseDTO<List<RecentPaymentsDTO>> response = payToMobileService.getRecentPayments(
                auth, ip, deviceId, latitude, longitude
        );

        return ResponseEntity.status(response.getResponseCode()).body(response);
    }


}
