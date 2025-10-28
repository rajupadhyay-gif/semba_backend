package com.banking.semba.controller;

import com.banking.semba.dto.ApiResponseDTO;
import com.banking.semba.dto.RecentPaymentsDTO;
import com.banking.semba.security.JwtTokenService;
import com.banking.semba.service.PayToUpiService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/pay/upi")
public class PayToUpiController {

    private final JwtTokenService jwtTokenService;
    private final PayToUpiService payToUpiService;

    public PayToUpiController(JwtTokenService jwtTokenService, PayToUpiService payToUpiService) {
        this.jwtTokenService = jwtTokenService;
        this.payToUpiService = payToUpiService;
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> searchByUpiId(
            @RequestHeader("Authorization") String auth,
            @RequestHeader("X-IP") String ip,
            @RequestHeader("X-Device-Id") String deviceId,
            @RequestHeader(value = "X-Latitude", required = false) Double latitude,
            @RequestHeader(value = "X-Longitude", required = false) Double longitude,
            @RequestParam(value = "upiId", required = false) String upiId
    )
         {
            ApiResponseDTO<Map<String, Object>> apiResponseDTO = new ApiResponseDTO<>();
            String mobile = jwtTokenService.extractMobileFromHeader(auth);
            if (mobile == null || mobile.isEmpty()) {
                return new ResponseEntity<>(apiResponseDTO, HttpStatus.UNAUTHORIZED);
            }
            ApiResponseDTO<Map<String, Object>> response =
                    payToUpiService.validateUpiId(auth, ip, deviceId, latitude, longitude, upiId);
            return ResponseEntity.status(response.getResponseCode()).body(response);
        }
    @GetMapping("/recent")
    public ResponseEntity<ApiResponseDTO<List<RecentPaymentsDTO>>> recentByUpiId(
            @RequestHeader("Authorization") String auth,
            @RequestHeader("X-IP") String ip,
            @RequestHeader("X-Device-Id") String deviceId,
            @RequestHeader(value = "X-Latitude", required = false) Double latitude,
            @RequestHeader(value = "X-Longitude", required = false) Double longitude,
            @RequestParam("upiId") String upiId) {

        ApiResponseDTO<List<RecentPaymentsDTO>> apiResponseDTO = new ApiResponseDTO<>();

        String mobile = jwtTokenService.extractMobileFromHeader(auth);
        if (mobile == null || mobile.isEmpty()) {
            return new ResponseEntity<>(apiResponseDTO, HttpStatus.UNAUTHORIZED);
        }

        ApiResponseDTO<List<RecentPaymentsDTO>> response =
                payToUpiService.getRecentPaymentsByUpiId(auth, ip, deviceId, latitude, longitude, upiId);

        return ResponseEntity.status(response.getResponseCode()).body(response);
    }

}

