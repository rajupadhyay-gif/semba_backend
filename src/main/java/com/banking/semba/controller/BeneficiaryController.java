package com.banking.semba.controller;


import com.banking.semba.constants.ValidationMessages;
import com.banking.semba.dto.BeneficiaryDTO;
import com.banking.semba.dto.HttpResponseDTO;
import com.banking.semba.dto.UpdateBeneficiaryDTO;
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


    @GetMapping("/fetch/payees")
    public ResponseEntity<HttpResponseDTO> getAllPayees(
            @RequestHeader("Authorization") String auth,
            @RequestHeader("X-IP") String ip,
            @RequestHeader("X-Device-Id") String deviceId,
            @RequestHeader(value = "X-Latitude", required = false) Double latitude,
            @RequestHeader(value = "X-Longitude", required = false) Double longitude
    ) {

        String mobile = jwtTokenService.extractMobileFromHeader(auth);
        if (mobile == null || mobile.isEmpty()) {
            HttpResponseDTO response = new HttpResponseDTO(
                    ValidationMessages.BAD_REQUEST,
                    HttpStatus.UNAUTHORIZED.value(),
                    ValidationMessages.USER_NOT_FOUND
            );
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        ResponseEntity<HttpResponseDTO> serviceResponse = beneficiaryService.getAllPayees(mobile, ip, deviceId, latitude, longitude);
        return serviceResponse;
    }

    @PutMapping("/update/payee/{payeeId}")
    public ResponseEntity<HttpResponseDTO> updatePayee(
            @RequestHeader("Authorization") String auth,
            @PathVariable Long payeeId,
            @RequestHeader("X-IP") String ip,
            @RequestHeader("X-Device-Id") String deviceId,
            @RequestHeader(value = "X-Latitude", required = false) Double latitude,
            @RequestHeader(value = "X-Longitude", required = false) Double longitude,
            @RequestBody UpdateBeneficiaryDTO updateBeneficiaryDTO
    ) {
        String mobile = jwtTokenService.extractMobileFromHeader(auth);
        if (mobile == null || mobile.isEmpty()) {
            HttpResponseDTO response = new HttpResponseDTO(ValidationMessages.BAD_REQUEST, HttpStatus.UNAUTHORIZED.value(), ValidationMessages.USER_NOT_FOUND);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        ResponseEntity<HttpResponseDTO> serviceResponse = beneficiaryService.updatePayee(
                mobile, ip, deviceId, latitude, longitude, payeeId, updateBeneficiaryDTO
        );
        return serviceResponse;
    }

    @DeleteMapping("/delete/payee/{payeeId}")
    public ResponseEntity<HttpResponseDTO> deletePayee(
            @RequestHeader("Authorization") String auth,
            @PathVariable Long payeeId,
            @RequestHeader("X-IP") String ip,
            @RequestHeader("X-Device-Id") String deviceId,
            @RequestHeader(value = "X-Latitude", required = false) Double latitude,
            @RequestHeader(value = "X-Longitude", required = false) Double longitude
    ) {
        String mobile = jwtTokenService.extractMobileFromHeader(auth);
        if (mobile == null || mobile.isEmpty()) {
            HttpResponseDTO response = new HttpResponseDTO(
                    ValidationMessages.BAD_REQUEST,
                    HttpStatus.UNAUTHORIZED.value(),
                    ValidationMessages.USER_NOT_FOUND
            );
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        ResponseEntity<HttpResponseDTO> serviceResponse = beneficiaryService.deletePayee(mobile, ip, deviceId, latitude, longitude, payeeId);
        return serviceResponse;
    }

}

