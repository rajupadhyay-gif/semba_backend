package com.banking.semba.service;

import com.banking.semba.constants.ValidationMessages;
import com.banking.semba.dto.BeneficiaryDTO;
import com.banking.semba.dto.HttpResponseDTO;
import com.banking.semba.util.UserServiceUtils;
import com.banking.semba.util.ValidationUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.banking.semba.GlobalException.GlobalExceptionHandler.badRequest;

@Slf4j
@Service
public class BeneficiaryService {

    private final ValidationUtil validationUtil;
    private final UserServiceUtils userUtils;

    public BeneficiaryService(ValidationUtil validationUtil, UserServiceUtils userUtils) {
        this.validationUtil = validationUtil;
        this.userUtils = userUtils;
    }

    private final WebClient webClient = WebClient.builder()
//            .baseUrl("https://api.paystack.co")//just for testing I integrated here
//            .baseUrl("https://ifsc.razorpay.com")
            .build();
    // Static bank info
    private static final List<String> BANKS = List.of(
            "HDFC", "ICICI", "SBI", "AXIS", "KOTAK MAHINDRA", "INDUSIND", "YES BANK", "IDFC FIRST",
            "BANDHAN", "FEDERAL BANK", "RBL BANK", "PNB", "CANARA BANK", "UNION BANK", "BANK OF INDIA",
            "INDIAN BANK", "CENTRAL BANK OF INDIA", "BANK OF BARODA", "UCO BANK", "SOUTH INDIAN BANK"
    );
    public ResponseEntity<HttpResponseDTO> addBeneficiary(
            String mobile, String ip, String deviceId,
            Double latitude, Double longitude,
            BeneficiaryDTO beneficiaryDTO) {
        String url = "https://jsonplaceholder.typicode.com/posts";
        userUtils.validateDeviceInfo(ip, deviceId, latitude, longitude, mobile);
        validationUtil.validateIpFormat(ip, mobile);
        validationUtil.validateDeviceIdFormat(deviceId, mobile);

        if (latitude != null && longitude != null) {
            validationUtil.validateLocation(latitude, String.valueOf(longitude), mobile);
        }

        if (beneficiaryDTO.getBeneficiaryName() == null || beneficiaryDTO.getBeneficiaryName().isBlank()) {
            return badRequest(ValidationMessages.BENEFICIARY_NAME_REQUIRED);
        }

        if (beneficiaryDTO.getBeneficiaryAccountNumber() == null ||
                !beneficiaryDTO.getBeneficiaryAccountNumber().matches("^\\d{9,18}$")) {
            return badRequest(ValidationMessages.ACCOUNT_NUMBER_REQUIRED);
        }

        if (!beneficiaryDTO.getBeneficiaryAccountNumber()
                .equals(beneficiaryDTO.getConfirmBeneficiaryAccountNumber())) {
            return badRequest(ValidationMessages.ACCOUNT_NUMBER_MISMATCH);
        }

        if (beneficiaryDTO.getBeneficiaryMobileNumber() == null ||
                !beneficiaryDTO.getBeneficiaryMobileNumber().matches("^[6-9]\\d{9}$")) {
            return badRequest(ValidationMessages.INVALID_MOBILE_FORMAT);
        }

        if (beneficiaryDTO.getIfscCode() == null ||
                !beneficiaryDTO.getIfscCode().matches("^[A-Z]{4}0[A-Z0-9]{6}$")) {
            return badRequest(ValidationMessages.INVALID_IFSC);
        }

        if (beneficiaryDTO.getBankId() == null || !BANKS.contains(beneficiaryDTO.getBankId())) {
            return badRequest(ValidationMessages.BANK_ID_REQUIRED);
        }

        try {
            Map<String, Object> requestBody = Map.of(
                    "beneficiaryName", beneficiaryDTO.getBeneficiaryName(),
                    "accountNumber", beneficiaryDTO.getBeneficiaryAccountNumber(),
                    "mobile", beneficiaryDTO.getBeneficiaryMobileNumber(),
                    "ifscCode", beneficiaryDTO.getIfscCode(),
                    "bankName", beneficiaryDTO.getBankId()
            );

            Map<String, Object> response = webClient.post()
                    .uri(url)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();

            HttpResponseDTO apiResponse = new HttpResponseDTO(ValidationMessages.STATUS_OK, HttpStatus.OK.value(), ValidationMessages.BENEFICIARY_ADDED_SUCCESSFULLY, response);
            return ResponseEntity.ok(apiResponse);

        } catch (Exception ex) {
            log.error("Error calling external bank API", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new HttpResponseDTO("FAILURE", HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to connect to bank API"));
        }
    }
}
