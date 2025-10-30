package com.banking.semba.util;

import com.banking.semba.globalException.GlobalException;
import com.banking.semba.constants.ValidationMessages;
import com.banking.semba.dto.OtpSendRequestDTO;
import com.banking.semba.dto.response.OtpResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Component
public class OtpUtil {

    private final WebClient bankWebClient;
    private static final boolean USE_MOCK = true;

    public OtpUtil(WebClient bankWebClient) {
        this.bankWebClient = bankWebClient;
    }

    //Build common headers
    public HttpHeaders buildHeaders(String mobile, String ip, String deviceId,
                                    Double latitude, Double longitude, boolean includeAuth) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-IP", ip);
        headers.set("X-Device-Id", deviceId);
        if (latitude != null) headers.set("X-Latitude", latitude.toString());
        if (longitude != null) headers.set("X-Longitude", longitude.toString());
        if (includeAuth && mobile != null) headers.set("Authorization", mobile);
        return headers;
    }


    // PUBLIC ENTRY POINT - decides between MOCK or REAL
    public OtpResponseDTO sendOtp(OtpSendRequestDTO request, HttpHeaders headers) {
        return USE_MOCK
                ? sendMockOtp(request)
                : sendOtpViaBank(request, headers);
    }

    // MOCK OTP - For development / testing
    private OtpResponseDTO sendMockOtp(OtpSendRequestDTO request) {
        log.info("[MOCK] Sending OTP | mobile={} | context={}", request.getMobile(), request.getContext());

        // static OTP for predictable testing; could use random if needed
        String otpCode = "123456";
        String message = buildOtpMessage(request.getContext(), otpCode);

        OtpResponseDTO mockResponse = OtpResponseDTO.builder()
                .mobile(request.getMobile())
                .otpCode(otpCode)
                .message(message)
                .success(true)
                .sentAt(LocalDateTime.now())
                .expirySeconds(300)
                .extra(Map.of(
                        "context", request.getContext(),
                        "referenceId", request.getReferenceId() != null ? request.getReferenceId() : "MOCK_REF_001",
                        "mockMode", true,
                        "expiresIn", "300 seconds"
                ))
                .build();

        log.info("[MOCK] OTP sent successfully | mobile={} | otp={}", request.getMobile(), otpCode);
        return mockResponse;
    }

    // REAL OTP - Call actual bank API
    public OtpResponseDTO sendOtpViaBank(OtpSendRequestDTO request, HttpHeaders headers) {
        try {
            log.info("[BANK] Sending OTP via API | mobile={} | context={}",
                    request.getMobile(), request.getContext());

            OtpResponseDTO response = bankWebClient.post()
                    .uri("/bank/otp/send")
                    .headers(h -> h.addAll(headers))
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(OtpResponseDTO.class)
                    .block();

            if (response == null)
                throw new GlobalException(ValidationMessages.BANK_API_FAILED, HttpStatus.INTERNAL_SERVER_ERROR.value());

            response.setSentAt(LocalDateTime.now());
            response.setExpirySeconds(300);

            log.info("[BANK] OTP sent successfully | mobile={} | context={}",
                    request.getMobile(), request.getContext());
            return response;

        } catch (WebClientResponseException ex) {
            log.error("Bank OTP API error: {} - {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new GlobalException(ValidationMessages.BANK_API_FAILED, ex.getStatusCode().value());
        } catch (Exception ex) {
            log.error("Unexpected OTP send error: {}", ex.getMessage());
            throw new GlobalException(ValidationMessages.OTP_SEND_FAILED, HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

    // -------------------------------------------------------
    // HELPER METHODS
    // -------------------------------------------------------
    public boolean isExpired(LocalDateTime createdAt, int expirySeconds) {
        return createdAt.plusSeconds(expirySeconds).isBefore(LocalDateTime.now());
    }

    public String buildOtpMessage(String context, String otpCode) {
        return switch (context == null ? "" : context.toUpperCase()) {
            case "TRANSFER" -> "OTP for confirming your fund transfer is " + otpCode;
            case "LOGIN" -> "Your login OTP is " + otpCode;
            case "BENEFICIARY" -> "OTP to add a new beneficiary is " + otpCode;
            default -> "Your OTP is " + otpCode;
        };
    }
}