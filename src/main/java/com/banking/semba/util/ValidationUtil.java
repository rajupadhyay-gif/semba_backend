package com.banking.semba.util;

import com.banking.semba.GlobalException.GlobalException;
import com.banking.semba.constants.LogMessages;
import com.banking.semba.constants.ValidationMessages;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import java.util.regex.Pattern;


@Slf4j
@Component
@Service
public class ValidationUtil {


//    // Regex for IPv4
//    private static final Pattern IP_PATTERN = Pattern.compile(
//            "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
//    );
//
//    // Regex for deviceId (alphanumeric + dash, 8–64 chars)
//    private static final Pattern DEVICE_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9\\-]{8,64}$");
//
//    // ---------- IP ----------
//    public void validateIpFormat(String ip, String mobile) {
//        if (ip == null || ip.isBlank() || !IP_PATTERN.matcher(ip).matches()) {
//            log.warn(LogMessages.IP_INVALID, mobile);
//            throw new IllegalArgumentException(ValidationMessages.IP_INVALID);
//        }
//    }
//
//    // ---------- Device ID ----------
//    public void validateDeviceIdFormat(String deviceId, String mobile) {
//        if (deviceId == null || deviceId.isBlank() || !DEVICE_ID_PATTERN.matcher(deviceId).matches()) {
//            log.warn(LogMessages.DEVICE_ID_INVALID, mobile);
//            throw new IllegalArgumentException(ValidationMessages.DEVICE_ID_INVALID);
//        }
//    }
//
//    // ---------- Location ----------
//    public void validateLocation(Double latitude, String longitude, String mobile) {
//        try {
//            double lat = Double.parseDouble(String.valueOf(latitude));
//            double lon = Double.parseDouble(String.valueOf(longitude));
//            if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
//                log.warn(LogMessages.LOCATION_INVALID, mobile);
//                throw new IllegalArgumentException(ValidationMessages.LOCATION_INVALID);
//            }
//        } catch (Exception e) {
//            log.warn(LogMessages.LOCATION_INVALID, mobile);
//            throw new IllegalArgumentException(ValidationMessages.LOCATION_INVALID);
//        }
//    }
//}

    private static final Pattern IP_PATTERN = Pattern.compile(
            "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}" +
                    "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
    );
    private static final Pattern DEVICE_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9\\-]{8,64}$");

    public void validateIpFormat(String ip, String mobile) {
        if (ip == null || ip.isBlank() || !IP_PATTERN.matcher(ip).matches()) {
            log.warn(LogMessages.IP_INVALID, mobile);
            throw new GlobalException(ValidationMessages.IP_INVALID, HttpStatus.BAD_REQUEST.value());
        }
    }

    public void validateDeviceIdFormat(String deviceId, String mobile) {
        if (deviceId == null || deviceId.isBlank() || !DEVICE_ID_PATTERN.matcher(deviceId).matches()) {
            log.warn(LogMessages.DEVICE_ID_INVALID, mobile);
            throw new GlobalException(ValidationMessages.DEVICE_ID_INVALID, HttpStatus.BAD_REQUEST.value());
        }
    }

    public void validateLocation(Double latitude, String longitude, String mobile) {
        try {
            double lat = Double.parseDouble(String.valueOf(latitude));
            double lon = Double.parseDouble(String.valueOf(longitude));
            if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
                log.warn(LogMessages.LOCATION_INVALID, mobile);
                throw new GlobalException(ValidationMessages.LOCATION_INVALID, HttpStatus.BAD_REQUEST.value());
            }
        } catch (Exception e) {
            log.warn(LogMessages.LOCATION_INVALID, mobile);
            throw new GlobalException(ValidationMessages.LOCATION_INVALID, HttpStatus.BAD_REQUEST.value());
        }
    }
}