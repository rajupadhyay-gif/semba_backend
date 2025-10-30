package com.banking.semba.service;

import com.banking.semba.globalException.GlobalException;
import com.banking.semba.constants.ValidationMessages;
import com.banking.semba.dto.HttpResponseDTO;
import com.banking.semba.dto.OtpSendRequestDTO;
import com.banking.semba.dto.response.OtpResponseDTO;
import com.banking.semba.security.JwtTokenService;
import com.banking.semba.util.OtpUtil;
import com.banking.semba.util.UserServiceUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private final OtpUtil otpUtil;
    private final JwtTokenService jwtTokenService;
    private final UserServiceUtils userUtils;

    private final Map<String, OtpResponseDTO> otpStore = new ConcurrentHashMap<>();
    private static final boolean USE_MOCK = false;

    // Send OTP (pre-login or post-login)
    public HttpResponseDTO sendOtp(String auth, String ip, String deviceId,
                                   Double latitude, Double longitude,
                                   OtpSendRequestDTO request, boolean isPreLogin) {
        String mobile = isPreLogin
                ? request.getMobile() // login OTP
                : jwtTokenService.extractMobileFromHeader(auth); // post-login OTP

        if (mobile == null || mobile.isBlank())
            throw new GlobalException(ValidationMessages.MOBILE_REQUIRED, HttpStatus.BAD_REQUEST.value());

        // Device info validation
        userUtils.validateDeviceInfo(ip, deviceId, latitude, longitude, mobile);

        if (USE_MOCK) {
            OtpResponseDTO mock = OtpResponseDTO.builder()
                    .mobile(mobile)
                    .otpCode("123456")
                    .message(otpUtil.buildOtpMessage(request.getContext(), "123456"))
                    .sentAt(LocalDateTime.now())
                    .expirySeconds(300)
                    .success(true)
                    .build();

            otpStore.put(mobile + "_" + request.getContext(), mock);
            return new HttpResponseDTO("SUCCESS", 200, "Mock OTP sent successfully.", mock);
        }

        // Real bank call
        var headers = otpUtil.buildHeaders(mobile, ip, deviceId, latitude, longitude, !isPreLogin);
        OtpResponseDTO bankResp = otpUtil.sendOtpViaBank(request, headers);
        otpStore.put(mobile + "_" + request.getContext(), bankResp);

        return new HttpResponseDTO(
                ValidationMessages.STATUS_OK,
                HttpStatus.OK.value(),
                ValidationMessages.OTP_SENT_SUCCESS,
                bankResp);
    }

//    // Verify OTP
//    public HttpResponseDTO verifyOtp(String auth, String ip, String deviceId,
//                                     Double latitude, Double longitude,
//                                     OtpVerifyRequestDTO request, boolean isPreLogin) {
//        String mobile = isPreLogin
//                ? request.getMobile()
//                : jwtTokenService.extractMobileFromHeader(auth);
//
//        String key = mobile + "_" + request.getContext();
//        OtpResponseDTO stored = otpStore.get(key);
//
//        if (stored == null)
//            throw new GlobalException(ValidationMessages.OTP_NOT_FOUND, 404);
//
//        if (otpUtil.isExpired(stored.getSentAt(), stored.getExpirySeconds()))
//            throw new GlobalException(ValidationMessages.OTP_EXPIRED, 400);
//
//        if (!stored.getOtpCode().equals(request.getOtpCode()))
//            throw new GlobalException(ValidationMessages.OTP_INVALID, 400);
//
//        otpStore.remove(key);
//        return new HttpResponseDTO("SUCCESS", 200, ValidationMessages.OTP_VERIFIED_SUCCESS, null);
//    }
}