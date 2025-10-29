package com.banking.semba.controller;


import com.banking.semba.constants.ValidationMessages;
import com.banking.semba.dto.*;
import com.banking.semba.security.JwtTokenService;
import com.banking.semba.service.BankService;
import com.banking.semba.service.BeneficiaryService;
import com.banking.semba.service.FundTransferService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("semba/api/beneficiary")
public class BeneficiaryController {
    private final JwtTokenService jwtTokenService;
    private final BeneficiaryService beneficiaryService;
    private final BankService bankService;
    private final FundTransferService fundTransferService;

    public BeneficiaryController(JwtTokenService jwtTokenService, BeneficiaryService beneficiaryService, BankService bankService, FundTransferService fundTransferService) {
        this.jwtTokenService = jwtTokenService;
        this.beneficiaryService = beneficiaryService;
        this.bankService = bankService;
        this.fundTransferService = fundTransferService;
    }

    @PostMapping("/add")
    public ResponseEntity<HttpResponseDTO> addBeneficiary(
            @RequestHeader("Authorization") String auth,
            @RequestHeader("X-IP") String ip,
            @RequestHeader("X-Device-Id") String deviceId,
            @RequestHeader(value = "X-Latitude", required = false) Double latitude,
            @RequestHeader(value = "X-Longitude", required = false) Double longitude,
            @Valid @RequestBody BeneficiaryDTO beneficiaryDTO
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

    @GetMapping("/fetch/topBanksList")
    public ResponseEntity<HttpResponseDTO> fetchTopBanksList(
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

        HttpResponseDTO response = bankService.fetchTopBanksList(auth, ip, deviceId, latitude, longitude);
        return ResponseEntity.status(response.getResponseCode()).body(response);
    }

    @GetMapping("/search/bankName")
    public ResponseEntity<HttpResponseDTO> searchBank(@RequestHeader("Authorization") String auth,
                                                      @RequestHeader("X-IP") String ip,
                                                      @RequestHeader("X-Device-Id") String deviceId,
                                                      @RequestHeader(value = "X-Latitude", required = false) Double latitude,
                                                      @RequestHeader(value = "X-Longitude", required = false) Double longitude,
                                                      @RequestParam String bankName) {
        HttpResponseDTO httpResponseDTO = new HttpResponseDTO();
        String mobile = jwtTokenService.extractMobileFromHeader(auth);
        if (mobile == null || mobile.isEmpty()) {
            return new ResponseEntity<>(httpResponseDTO, HttpStatus.UNAUTHORIZED);
        }
        HttpResponseDTO response = bankService.searchBanks(auth, ip, deviceId, latitude, longitude, bankName);
        return ResponseEntity.status(response.getResponseCode()).body(response);
    }

    @PostMapping("/bank-Transfer/initiate")
    public ResponseEntity<HttpResponseDTO> initiateTransfer(
            @RequestHeader("Authorization") String auth,
            @RequestHeader("X-IP") String ip,
            @RequestHeader("X-Device-Id") String deviceId,
            @RequestHeader(value = "X-Latitude", required = false) Double latitude,
            @RequestHeader(value = "X-Longitude", required = false) Double longitude,
            @Valid @RequestBody FundTransferRequestDTO request) {

        HttpResponseDTO httpResponseDTO = new HttpResponseDTO();
        String mobile = jwtTokenService.extractMobileFromHeader(auth);
        if (mobile == null || mobile.isEmpty()) {
            return new ResponseEntity<>(httpResponseDTO, HttpStatus.UNAUTHORIZED);
        }
        HttpResponseDTO response = fundTransferService.initiateTransfer(mobile, ip, deviceId, latitude, longitude, request);
        return ResponseEntity.status(response.getResponseCode()).body(response);
    }

    @PostMapping("/bank-Transfer/verify-otp")
    public ResponseEntity<HttpResponseDTO> verifyOtp(
            @RequestHeader("Authorization") String auth,
            @RequestHeader("X-IP") String ip,
            @RequestHeader("X-Device-Id") String deviceId,
            @RequestHeader(value = "X-Latitude", required = false) Double latitude,
            @RequestHeader(value = "X-Longitude", required = false) Double longitude,
            @Valid @RequestBody OtpVerifyRequestDTO otpRequest) {

        HttpResponseDTO httpResponseDTO = new HttpResponseDTO();
        String mobile = jwtTokenService.extractMobileFromHeader(auth);
        if (mobile == null || mobile.isEmpty()) {
            return new ResponseEntity<>(httpResponseDTO, HttpStatus.UNAUTHORIZED);
        }

        HttpResponseDTO response = fundTransferService.verifyOtp(mobile, ip, deviceId, latitude, longitude, otpRequest);
        return ResponseEntity.status(response.getResponseCode()).body(response);
    }
}
