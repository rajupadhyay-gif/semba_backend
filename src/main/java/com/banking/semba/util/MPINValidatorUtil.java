package com.banking.semba.util;

import com.banking.semba.constants.ValidationMessages;
import com.banking.semba.dto.ApiResponseDTO;
import com.banking.semba.dto.MPINValidationResponseDTO;
import com.banking.semba.security.JwtTokenService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

@Component
public class MPINValidatorUtil {

    private final WebClient webClient;
    private final JwtTokenService jwtTokenService;
    private final UserServiceUtils userUtils;
    private final ValidationUtil validationUtil;

    public MPINValidatorUtil(WebClient webClient, JwtTokenService jwtTokenService, UserServiceUtils userUtils, ValidationUtil validationUtil) {
        this.webClient = webClient;
        this.jwtTokenService = jwtTokenService;
        this.userUtils = userUtils;
        this.validationUtil = validationUtil;
    }

    private void validateDevice(String ip, String deviceId, Double latitude, Double longitude, String mobile) {
        userUtils.validateDeviceInfo(ip, deviceId, latitude, longitude, mobile);
        validationUtil.validateIpFormat(ip, mobile);
        validationUtil.validateDeviceIdFormat(deviceId, mobile);
        if (latitude != null && longitude != null) {
            validationUtil.validateLocation(latitude, String.valueOf(longitude), mobile);
        }
    }

    public ApiResponseDTO<MPINValidationResponseDTO> validateMPIN(
            String auth,
            String ip,
            String deviceId,
            Double latitude,
            Double longitude,
            String accountNumber,
            String mpin,
            String transactionId
    ) {
        String mobile = jwtTokenService.extractMobileFromHeader(auth);
        if (mobile == null || mobile.isEmpty()) {
            return new ApiResponseDTO<>(
                    ValidationMessages.STATUS_UNAUTHORIZED,
                    HttpStatus.UNAUTHORIZED.value(),
                    ValidationMessages.INVALID_JWT,
                    null
            );
        }

        validateDevice(ip, deviceId, latitude, longitude, mobile);

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("accountNumber", accountNumber);
            requestBody.put("mpin", mpin);
            requestBody.put("transactionId", transactionId);

            String dummyMpinUrl = "https://dummy-bank-api.com/api/validateMpin";

            MPINValidationResponseDTO bankResponse = webClient
                    .post()
                    .uri(dummyMpinUrl)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(MPINValidationResponseDTO.class)
                    .onErrorResume(ex -> {
                        MPINValidationResponseDTO fallbackResponse = new MPINValidationResponseDTO();
                        fallbackResponse.setValid("1234".equals(mpin));
                        fallbackResponse.setMessage(fallbackResponse.isValid()
                                ? "Dummy MPIN validation success"
                                : "Dummy MPIN validation failed");
                        return reactor.core.publisher.Mono.just(fallbackResponse);
                    })
                    .block();

            if (bankResponse != null && bankResponse.isValid()) {
                return new ApiResponseDTO<>(
                        "SUCCESS",
                        HttpStatus.OK.value(),
                        "MPIN validated successfully for Transaction ID: " + transactionId,
                        bankResponse
                );
            } else {
                return new ApiResponseDTO<>(
                        "FAILED",
                        HttpStatus.BAD_REQUEST.value(),
                        "Invalid MPIN for Transaction ID: " + transactionId,
                        bankResponse
                );
            }

        } catch (Exception e) {
            return new ApiResponseDTO<>(
                    "ERROR",
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "MPIN validation failed: " + e.getMessage(),
                    null
            );
        }
    }
}
