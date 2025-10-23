package com.banking.semba.service;

import com.banking.semba.GlobalException.GlobalException;
import com.banking.semba.constants.LogMessages;
import com.banking.semba.constants.ValidationMessages;
import com.banking.semba.dto.ApiResponseDTO;
import com.banking.semba.dto.CardOtpRequest;
import com.banking.semba.dto.CardRequest;
import com.banking.semba.dto.response.BankCardResponse;
import com.banking.semba.util.UserServiceUtils;
import com.banking.semba.util.ValidationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CardService {

    private final WebClient bankWebClient;
    private final UserServiceUtils userUtils;
    private final ValidationUtil validationUtil;

    private static final String BASE_CARD_URL = "https://jsonplaceholder.typicode.com";
    private static final String Verify_BASE_CARD_URL = "https://jsonplaceholder.typicode.com";
    private static final String getBaseCardUrl = "https://ifsc.razorpay.com";


    private static final boolean useMock = true;

    private static final int MAX_OTP_ATTEMPTS = 3;
    private static final String DEMO_OTP = "123456";

    public ApiResponseDTO<Map<String, Object>> addCard(CardRequest req,
                                                       String mobile,
                                                       String ip,
                                                       String deviceId,
                                                       Double latitude,
                                                       Double longitude) {
        log.info(LogMessages.CARD_ADD_REQUEST, mobile, maskPan(req.getCardNumber()));
        validateRequest(mobile, ip, deviceId, latitude, longitude);

        // ---------- Basic validations ----------
        if (req.getCardNumber() == null || req.getCardNumber().isBlank()) {
            throw new GlobalException(ValidationMessages.CARD_ID_BLANK, HttpStatus.BAD_REQUEST.value());
        }
        if (!is16Digits(req.getCardNumber())) {
            throw new GlobalException(ValidationMessages.CARD_ID_INVALID, HttpStatus.BAD_REQUEST.value());
        }
        if (req.getHolderName() == null || req.getHolderName().isBlank()) {
            throw new GlobalException(ValidationMessages.HOLDER_NAME_BLANK, HttpStatus.BAD_REQUEST.value());
        }
        if (!req.getHolderName().matches("^[A-Za-z ]{2,50}$")) {
            throw new GlobalException(ValidationMessages.HOLDER_NAME_INVALID, HttpStatus.BAD_REQUEST.value());
        }
        if (req.getValidThru() == null || req.getValidThru().isBlank()) {
            throw new GlobalException(ValidationMessages.VALID_THRU_BLANK, HttpStatus.BAD_REQUEST.value());
        }
        if (!isValidExpiry(req.getValidThru())) {
            throw new GlobalException(ValidationMessages.VALID_THRU_INVALID, HttpStatus.BAD_REQUEST.value());
        }

        // ---------- MOCK Response ----------
        if (useMock) {
            Map<String, Object> mockCard = new HashMap<>();
            mockCard.put("mobile", mobile);
            mockCard.put("cardNumber", maskPan(req.getCardNumber()));
            mockCard.put("holderName", req.getHolderName());
            mockCard.put("validThru", req.getValidThru());
            mockCard.put("verified", false);
            mockCard.put("otpSentAt", LocalDateTime.now());

            Map<String, Object> data = new HashMap<>();
            data.put("card", mockCard);

            log.info(LogMessages.CARD_ADD_SUCCESS, mobile, maskPan(req.getCardNumber()));
            return new ApiResponseDTO<>("SUCCESS", HttpStatus.OK.value(),
                    ValidationMessages.CARD_ADDED_SUCCESS, data);
        }

        // ---------- REAL BANK CALL ----------
        try {
            BankCardResponse bankResponse = bankWebClient.post()
                    .uri(getBaseCardUrl)
                    .headers(h -> {
                        h.set(HttpHeaders.AUTHORIZATION, mobile);
                        h.set("X-Device-Id", deviceId);
                        h.set("X-IP", ip);
                        if (latitude != null) h.set("X-Latitude", latitude.toString());
                        if (longitude != null) h.set("X-Longitude", longitude.toString());
                        h.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
                    })
                    .bodyValue(req)
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(),
                            clientResponse -> Mono.error(new GlobalException(
                                    ValidationMessages.BANK_API_FAILED,
                                    clientResponse.statusCode().value())))
                    .bodyToMono(BankCardResponse.class)
                    .block();

            if (bankResponse == null || !bankResponse.isSuccess()) {
                throw new GlobalException(ValidationMessages.CARD_ADD_FAILED, HttpStatus.BAD_REQUEST.value());
            }

            Map<String, Object> data = new HashMap<>();
            data.put("card", bankResponse.getCard());
            return new ApiResponseDTO<>(
                    "SUCCESS",
                    HttpStatus.OK.value(),
                    ValidationMessages.CARD_ADDED_SUCCESS,
                    data);

        } catch (WebClientResponseException ex) {
            log.error(LogMessages.BANK_API_ERROR, ex.getStatusCode().value(), ex.getResponseBodyAsString());
            throw new GlobalException(ValidationMessages.BANK_API_FAILED, ex.getStatusCode().value());
        }
    }

    /**
     * Verify Card OTP (with validation)
     */
    public ApiResponseDTO<Map<String, Object>> verifyCardOtp(CardOtpRequest req,
                                                             String mobile,
                                                             String ip,
                                                             String deviceId,
                                                             Double latitude,
                                                             Double longitude) {
        log.info(LogMessages.OTP_VERIFY_REQUEST, mobile, maskPan(req.getCardNumber()));
        validateRequest(mobile, ip, deviceId, latitude, longitude);

        // ---------- OTP validation ----------
        if (req.getOtp() == null || req.getOtp().isBlank()) {
            throw new GlobalException(ValidationMessages.OTP_BLANK, HttpStatus.BAD_REQUEST.value());
        }
        if (!req.getOtp().matches("\\d{6}")) {
            throw new GlobalException(ValidationMessages.OTP_FORMAT_INVALID, HttpStatus.BAD_REQUEST.value());
        }

        if (useMock) {
            if (!DEMO_OTP.equals(req.getOtp())) {
                log.warn(LogMessages.OTP_VERIFY_FAILED, mobile, "invalid-otp");
                throw new GlobalException(ValidationMessages.OTP_INVALID, HttpStatus.BAD_REQUEST.value());
            }

            Map<String, Object> data = new HashMap<>();
            data.put("verified", true);
            data.put("verifiedAt", LocalDateTime.now());
            log.info(LogMessages.OTP_VERIFY_SUCCESS, mobile, maskPan(req.getCardNumber()));
            return new ApiResponseDTO<>("SUCCESS", HttpStatus.OK.value(),
                    ValidationMessages.CARD_OTP_VERIFY_SUCCESS, data);
        }

        try {
            BankCardResponse bankResponse = bankWebClient.post()
                    .uri(Verify_BASE_CARD_URL)
                    .headers(h -> {
                        h.set(HttpHeaders.AUTHORIZATION, mobile);
                        h.set("X-Device-Id", deviceId);
                        h.set("X-IP", ip);
                        if (latitude != null) h.set("X-Latitude", latitude.toString());
                        if (longitude != null) h.set("X-Longitude", longitude.toString());
                        h.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
                    })
                    .bodyValue(req)
                    .retrieve()
                    .bodyToMono(BankCardResponse.class)
                    .block();

            if (bankResponse == null || !bankResponse.isSuccess()) {
                throw new GlobalException(ValidationMessages.OTP_INVALID, HttpStatus.BAD_REQUEST.value());
            }

            Map<String, Object> data = new HashMap<>();
            data.put("verified", true);
            data.put("verifiedAt", LocalDateTime.now());
            return new ApiResponseDTO<>(
                    "SUCCESS",
                    HttpStatus.OK.value(),
                    ValidationMessages.CARD_OTP_VERIFY_SUCCESS,
                    data);

        } catch (WebClientResponseException ex) {
            log.error(LogMessages.BANK_API_ERROR, ex.getStatusCode().value(), ex.getResponseBodyAsString());
            throw new GlobalException(ValidationMessages.BANK_API_FAILED, ex.getStatusCode().value());
        }
    }

    /**
     * Get All Cards
     */
    public ApiResponseDTO<Map<String, Object>> getCards(String mobile,
                                                        String ip,
                                                        String deviceId,
                                                        Double latitude,
                                                        Double longitude) {
        log.info(LogMessages.GET_CARDS, mobile);
        validateRequest(mobile, ip, deviceId, latitude, longitude);

        if (useMock) {
            Map<String, Object> card = new HashMap<>();
            card.put("cardNumber", "************4321");
            card.put("holderName", "John Doe");
            card.put("validThru", "12/26");
            card.put("verified", true);

            Map<String, Object> data = new HashMap<>();
            data.put("cards", card);

            return new ApiResponseDTO<>("SUCCESS", HttpStatus.OK.value(),
                    ValidationMessages.CARDS_LIST_FETCH_SUCCESS, data);
        }

        try {
            BankCardResponse bankResponse = bankWebClient.get()
                    .uri(BASE_CARD_URL)
                    .header(HttpHeaders.AUTHORIZATION, mobile)
                    .retrieve()
                    .bodyToMono(BankCardResponse.class)
                    .block();

            if (bankResponse == null || !bankResponse.isSuccess())
                throw new GlobalException(ValidationMessages.CARDS_FETCH_FAILED, HttpStatus.BAD_REQUEST.value());

            Map<String, Object> data = new HashMap<>();
            data.put("cards", bankResponse.getCards());
            return new ApiResponseDTO<>(
                    "SUCCESS",
                    HttpStatus.OK.value(),
                    ValidationMessages.CARDS_LIST_FETCH_SUCCESS,
                    data);

        } catch (WebClientResponseException ex) {
            throw new GlobalException(ValidationMessages.BANK_API_FAILED, ex.getStatusCode().value());
        }
    }

    // ---------- Shared Helpers ----------

    private void validateRequest(String mobile, String ip, String deviceId, Double latitude, Double longitude) {
        userUtils.validateDeviceInfo(ip, deviceId, latitude, longitude, mobile);
        validationUtil.validateIpFormat(ip, mobile);
        validationUtil.validateDeviceIdFormat(deviceId, mobile);
        if (latitude != null && longitude != null)
            validationUtil.validateLocation(latitude, String.valueOf(longitude), mobile);
    }

    private String maskPan(String pan) {
        if (pan == null || pan.length() < 4) return "****";
        return "************" + pan.substring(pan.length() - 4);
    }

    private boolean is16Digits(String num) {
        return num.matches("\\d{16}");
    }

    private boolean isValidExpiry(String expiry) {
        return expiry.matches("^(0[1-9]|1[0-2])\\/\\d{2}$");
    }
}