package com.banking.semba.service;

import com.banking.semba.GlobalException.GlobalException;
import com.banking.semba.constants.LogMessages;
import com.banking.semba.constants.ValidationMessages;
import com.banking.semba.dto.ApiResponseDTO;
import com.banking.semba.dto.CardOtpRequest;
import com.banking.semba.dto.CardRequest;
import com.banking.semba.dto.PayNowRequest;
import com.banking.semba.dto.response.BankCardResponse;
import com.banking.semba.dto.response.BankTransactionResponse;
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
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CardService {

    private final WebClient bankWebClient;
    private final UserServiceUtils userUtils;
    private final ValidationUtil validationUtil;

    private static final String PROD_CARD_URL = "https://api.bank.com/cards";
    private static final String PROD_VERIFY_CARD_URL = "https://api.bank.com/cards/verify";
    private static final String PROD_TRANSACTION_URL = "https://api.bank.com/debit";

    private static final boolean USE_MOCK = true;
    private static final String DEMO_OTP = "123456";
    private static final String DEMO_MPIN = "1234";

    // ---------------- Add Card ----------------
    public ApiResponseDTO<Map<String, Object>> addCard(CardRequest req,
                                                       String mobile,
                                                       String ip,
                                                       String deviceId,
                                                       Double latitude,
                                                       Double longitude) {
        log.info(LogMessages.CARD_ADD_REQUEST, mobile, maskPan(req.getCardNumber()));
        validateRequest(mobile, ip, deviceId, latitude, longitude);
        validateCardDetails(req);

        if (USE_MOCK) return buildMockCardResponse(req, mobile, false);

        try {
            BankCardResponse bankResponse = bankWebClient.post()
                    .uri(PROD_CARD_URL)
                    .headers(h -> setBankHeaders(h, mobile, ip, deviceId, latitude, longitude))
                    .bodyValue(req)
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(),
                            resp -> Mono.error(new GlobalException(ValidationMessages.BANK_API_FAILED,
                                    resp.statusCode().value())))
                    .bodyToMono(BankCardResponse.class)
                    .block();

            if (bankResponse == null || !bankResponse.isSuccess())
                throw new GlobalException(ValidationMessages.CARD_ADD_FAILED, HttpStatus.BAD_REQUEST.value());

            return buildResponse(bankResponse.getCard(), ValidationMessages.CARD_ADDED_SUCCESS);

        } catch (WebClientResponseException ex) {
            log.error(LogMessages.BANK_API_ERROR, ex.getStatusCode().value(), ex.getResponseBodyAsString());
            throw new GlobalException(ValidationMessages.BANK_API_FAILED, ex.getStatusCode().value());
        }
    }

    // ---------------- Verify Card OTP ----------------
    public ApiResponseDTO<Map<String, Object>> verifyCardOtp(CardOtpRequest req,
                                                             String mobile,
                                                             String ip,
                                                             String deviceId,
                                                             Double latitude,
                                                             Double longitude) {
        log.info(LogMessages.OTP_VERIFY_REQUEST, mobile, maskPan(req.getCardNumber()));
        validateRequest(mobile, ip, deviceId, latitude, longitude);
        validateOtp(req.getOtp());

        if (USE_MOCK) {
            if (!DEMO_OTP.equals(req.getOtp())) {
                log.warn(LogMessages.OTP_VERIFY_FAILED, mobile, "invalid-otp");
                throw new GlobalException(ValidationMessages.OTP_INVALID, HttpStatus.BAD_REQUEST.value());
            }
            return buildOtpResponse();
        }
        try {
            BankCardResponse bankResponse = bankWebClient.post()
                    .uri(PROD_VERIFY_CARD_URL)
                    .headers(h -> setBankHeaders(h, mobile, ip, deviceId, latitude, longitude))
                    .bodyValue(req)
                    .retrieve()
                    .bodyToMono(BankCardResponse.class)
                    .block();

            if (bankResponse == null || !bankResponse.isSuccess())
                throw new GlobalException(ValidationMessages.OTP_INVALID, HttpStatus.BAD_REQUEST.value());

            return buildOtpResponse();

        } catch (WebClientResponseException ex) {
            log.error(LogMessages.BANK_API_ERROR, ex.getStatusCode().value(), ex.getResponseBodyAsString());
            throw new GlobalException(ValidationMessages.BANK_API_FAILED, ex.getStatusCode().value());
        }
    }

    // ---------------- Get Cards ----------------
    public ApiResponseDTO<Map<String, Object>> getCards(String mobile,
                                                        String ip,
                                                        String deviceId,
                                                        Double latitude,
                                                        Double longitude,
                                                        String type) {
        log.info(LogMessages.GET_CARDS, mobile);
        validateRequest(mobile, ip, deviceId, latitude, longitude);

        List<BankCardResponse.CardDetail> allCards;

        // ---------------- MOCK DATA ----------------
        if (USE_MOCK) {
            BankCardResponse.CardDetail creditCard = new BankCardResponse.CardDetail();
            creditCard.setCardNumber("************4321");
            creditCard.setHolderName("John Doe");
            creditCard.setValidThru("12/26");
            creditCard.setVerified(true);
            creditCard.setOtpSentAt(null); // optional
            creditCard.setAddedAt(LocalDateTime.now());
            creditCard.setMetadata(Map.of("type", "CREDIT"));

            BankCardResponse.CardDetail debitCard = new BankCardResponse.CardDetail();
            debitCard.setCardNumber("************5678");
            debitCard.setHolderName("Alice Brown");
            debitCard.setValidThru("08/25");
            debitCard.setVerified(true);
            debitCard.setOtpSentAt(null);
            debitCard.setAddedAt(LocalDateTime.now());
            debitCard.setMetadata(Map.of("type", "DEBIT"));

            allCards = List.of(creditCard, debitCard);
        }
        // ---------------- REAL BANK CALL ----------------
        else {
            BankCardResponse bankResponse = bankWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(PROD_CARD_URL)
                            .queryParam("type", "DEBIT,CREDIT") // fetch all
                            .build())
                    .headers(h -> setBankHeaders(h, mobile, ip, deviceId, latitude, longitude))
                    .retrieve()
                    .bodyToMono(BankCardResponse.class)
                    .block();

            if (bankResponse == null || !bankResponse.isSuccess())
                throw new GlobalException(ValidationMessages.CARDS_FETCH_FAILED, HttpStatus.BAD_REQUEST.value());

            allCards = bankResponse.getCards();
        }

        // ---------------- FILTER BY TYPE ----------------
        if (type != null && !type.isBlank()) {
            String typeUpper = type.toUpperCase();
            allCards = allCards.stream()
                    .filter(card -> typeUpper.equalsIgnoreCase((String) card.getMetadata().get("type")))
                    .toList();
        }

        // ---------------- BUILD RESPONSE ----------------
        Map<String, Object> data = new HashMap<>();
        data.put("cards", allCards);

        log.info("Cards fetched successfully for mobile: {}. Total cards: {}", mobile, allCards.size());
        return new ApiResponseDTO<>("SUCCESS",
                HttpStatus.OK.value(),
                ValidationMessages.CARDS_LIST_FETCH_SUCCESS,
                data);
    }
    // ---------------- Process Payment ----------------
    public ApiResponseDTO<Map<String, Object>> processPayment(PayNowRequest req,
                                                              String mobile,
                                                              String ip,
                                                              String deviceId,
                                                              Double latitude,
                                                              Double longitude) {
        log.info(LogMessages.PAYMENT_REQUEST, mobile, maskPan(req.getCardNumber()));
        validateRequest(mobile, ip, deviceId, latitude, longitude);
        validateCardForPayment(req);

        if (USE_MOCK) {
            Map<String, Object> data = new HashMap<>();
            data.put("transactionId", "TXN" + System.currentTimeMillis());
            data.put("amount", req.getAmount());
            data.put("status", "SUCCESS");
            data.put("timestamp", LocalDateTime.now());
            log.info(LogMessages.PAYMENT_SUCCESS, mobile, maskPan(req.getCardNumber()));
            return new ApiResponseDTO<>("SUCCESS", HttpStatus.OK.value(),
                    ValidationMessages.PAYMENT_SUCCESS, data);
        }

        try {
            BankTransactionResponse bankResponse = bankWebClient.post()
                    .uri(PROD_TRANSACTION_URL)
                    .headers(h -> setBankHeaders(h, mobile, ip, deviceId, latitude, longitude))
                    .bodyValue(req)
                    .retrieve()
                    .bodyToMono(BankTransactionResponse.class)
                    .block();

            if (bankResponse == null || !bankResponse.isSuccess())
                throw new GlobalException("Bank transaction failed", HttpStatus.BAD_REQUEST.value());

            Map<String, Object> data = new HashMap<>();
            data.put("transactionId", bankResponse.getTransactionId());
            data.put("amount", req.getAmount());
            data.put("status", "SUCCESS");
            data.put("timestamp", LocalDateTime.now());
            return new ApiResponseDTO<>("SUCCESS", HttpStatus.OK.value(),
                    ValidationMessages.PAYMENT_SUCCESS, data);

        } catch (WebClientResponseException ex) {
            log.error(LogMessages.BANK_API_ERROR, ex.getStatusCode().value(), ex.getResponseBodyAsString());
            throw new GlobalException("Bank API failed", ex.getStatusCode().value());
        }
    }

    // ---------------- Helpers ----------------
    private void setBankHeaders(HttpHeaders headers, String mobile, String ip, String deviceId, Double lat, Double lon) {
        headers.set(HttpHeaders.AUTHORIZATION, mobile);
        headers.set("X-Device-Id", deviceId);
        headers.set("X-IP", ip);
        if (lat != null) headers.set("X-Latitude", lat.toString());
        if (lon != null) headers.set("X-Longitude", lon.toString());
        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
    }

    private void validateRequest(String mobile, String ip, String deviceId, Double latitude, Double longitude) {
        userUtils.validateDeviceInfo(ip, deviceId, latitude, longitude, mobile);
        validationUtil.validateIpFormat(ip, mobile);
        validationUtil.validateDeviceIdFormat(deviceId, mobile);
        if (latitude != null && longitude != null)
            validationUtil.validateLocation(latitude, String.valueOf(longitude), mobile);
    }

    private void validateCardDetails(CardRequest req) {
        if (req.getCardNumber() == null || req.getCardNumber().isBlank())
            throw new GlobalException(ValidationMessages.CARD_ID_BLANK, HttpStatus.BAD_REQUEST.value());
        if (!req.getCardNumber().matches("\\d{16}"))
            throw new GlobalException(ValidationMessages.CARD_ID_INVALID, HttpStatus.BAD_REQUEST.value());
        if (req.getHolderName() == null || req.getHolderName().isBlank())
            throw new GlobalException(ValidationMessages.HOLDER_NAME_BLANK, HttpStatus.BAD_REQUEST.value());
        if (!req.getHolderName().matches("^[A-Za-z ]{2,50}$"))
            throw new GlobalException(ValidationMessages.HOLDER_NAME_INVALID, HttpStatus.BAD_REQUEST.value());
        if (req.getValidThru() == null || req.getValidThru().isBlank())
            throw new GlobalException(ValidationMessages.VALID_THRU_BLANK, HttpStatus.BAD_REQUEST.value());
        if (!req.getValidThru().matches("^(0[1-9]|1[0-2])/\\d{2}$"))
            throw new GlobalException(ValidationMessages.VALID_THRU_INVALID, HttpStatus.BAD_REQUEST.value());
    }

    private void validateCardForPayment(PayNowRequest req) {
        if (req.getCardNumber() == null || req.getCardNumber().isBlank())
            throw new GlobalException(ValidationMessages.CARD_ID_BLANK, HttpStatus.BAD_REQUEST.value());
        if (!req.getCardNumber().matches("\\d{16}"))
            throw new GlobalException(ValidationMessages.CARD_ID_INVALID, HttpStatus.BAD_REQUEST.value());
        if (req.getValidThru() == null || req.getValidThru().isBlank())
            throw new GlobalException(ValidationMessages.VALID_THRU_BLANK, HttpStatus.BAD_REQUEST.value());
        if (!req.getValidThru().matches("^(0[1-9]|1[0-2])/\\d{2}$"))
            throw new GlobalException(ValidationMessages.VALID_THRU_INVALID, HttpStatus.BAD_REQUEST.value());
        if (!DEMO_MPIN.equals(req.getMpin()))
            throw new GlobalException("Invalid MPIN", HttpStatus.BAD_REQUEST.value());
    }

    private void validateOtp(String otp) {
        if (otp == null || otp.isBlank())
            throw new GlobalException(ValidationMessages.OTP_BLANK, HttpStatus.BAD_REQUEST.value());
        if (!otp.matches("\\d{6}"))
            throw new GlobalException(ValidationMessages.OTP_FORMAT_INVALID, HttpStatus.BAD_REQUEST.value());
    }

    private String maskPan(String pan) {
        if (pan == null || pan.length() < 4) return "****";
        return "************" + pan.substring(pan.length() - 4);
    }

    private ApiResponseDTO<Map<String, Object>> buildMockCardResponse(CardRequest req, String mobile, boolean verified) {
        Map<String, Object> card = new HashMap<>();
        card.put("mobile", mobile);
        card.put("cardNumber", maskPan(req.getCardNumber()));
        card.put("holderName", req.getHolderName());
        card.put("validThru", req.getValidThru());
        card.put("verified", verified);
        card.put("otpSentAt", LocalDateTime.now());
        return buildResponse(card, ValidationMessages.CARD_ADDED_SUCCESS);
    }

    private ApiResponseDTO<Map<String, Object>> buildOtpResponse() {
        Map<String, Object> data = new HashMap<>();
        data.put("verified", true);
        data.put("verifiedAt", LocalDateTime.now());
        return new ApiResponseDTO<>(
                "SUCCESS",
                HttpStatus.OK.value(),
                ValidationMessages.CARD_OTP_VERIFY_SUCCESS,
                data);
    }

    private ApiResponseDTO<Map<String, Object>> buildResponse(Object payload, String message) {
        Map<String, Object> data = new HashMap<>();
        data.put("data", payload);
        return new ApiResponseDTO<>(
                "SUCCESS",
                HttpStatus.OK.value(),
                message,
                data);
    }
}