package com.banking.semba.controller;


import com.banking.semba.constants.ValidationMessages;
import com.banking.semba.dto.BeneficiaryDTO;
import com.banking.semba.dto.HttpResponseDTO;
import com.banking.semba.security.JwtTokenService;
import com.banking.semba.service.BeneficiaryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("semba/api/beneficiary")
public class BeneficiaryController {
    private final JwtTokenService jwtTokenService;
    private final BeneficiaryService beneficiaryService;

    public BeneficiaryController(JwtTokenService jwtTokenService, BeneficiaryService beneficiaryService) {
        this.jwtTokenService = jwtTokenService;
        this.beneficiaryService = beneficiaryService;
    }

    @PostMapping("/add")
    public ResponseEntity<HttpResponseDTO> addBeneficiary(
            @RequestHeader("Authorization") String auth,
            @RequestHeader("X-IP") String ip,
            @RequestHeader("X-Device-Id") String deviceId,
            @RequestHeader(value = "X-Latitude", required = false) Double latitude,
            @RequestHeader(value = "X-Longitude", required = false) Double longitude,
            @Valid @RequestBody com.banking.semba.dto.BeneficiaryDTO beneficiaryDTO
    ) {
        String mobile = jwtTokenService.extractMobileFromHeader(auth);
        if (mobile == null || mobile.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new HttpResponseDTO(ValidationMessages.BAD_REQUEST, HttpStatus.UNAUTHORIZED.value(), ValidationMessages.USER_NOT_FOUND));
        }
        ResponseEntity<HttpResponseDTO> serviceResponse = beneficiaryService.addBeneficiary(mobile, ip, deviceId, latitude, longitude, beneficiaryDTO);
        return serviceResponse;
    }


}

