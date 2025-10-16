package com.banking.semba.constants;

public class ValidationMessages {

    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";

    private ValidationMessages() {
    }

    public static final String MOBILE_BLANK = "Mobile number cannot be blank";
    public static final String MOBILE_INVALID_PATTERN = "Mobile number must be exactly 10 digits";
    public static final String MOBILE_ALREADY_REGISTERED = "Mobile number already registered";
    public static final String BANKING_FAILED = "Bank API error";
    public static final String OTP_SENT_SUCCESS = "OTP sent successfully";
    public static final String ERROR_CALL_API = " Unexpected error while calling bank API";

    public static final String JWT_EXPIRED = "JWT token in expired";
    public static final String JWT_EXPIREDD = "JWT token in expired";
    //Verify Otp
    public static final String USER_NOT_FOUND = "User not found";
    public static final String OTP_BLANK = "OTP cannot be blank";
    public static final String OTP_INVALID = "Invalid OTP";
    public static final String OTP_FAILED_BANK ="Unexpected error while calling bank API";

    public static final String OTP_VERIFIED_SUCCESS = "OTP verified successfully";

    //Resend OTP
    public static final String MOBILE_ALREADY_VERIFIED = "Mobile already verified, please login";
    public static final String OTP_RESENT_SUCCESS = "OTP resent successfully";

    // MPIN related
    public static final String MPIN_BLANK = "MPIN cannot be blank";
    public static final String CONFIRM_MPIN_BLANK = "Confirm MPIN cannot be blank";
    public static final String MPIN_NOT_MATCH = "MPIN and Confirm MPIN do not match";
    public static final String MPIN_SET_SUCCESS = "MPIN set successfully";
    public static final String MPIN_INVALID_PATTERN = "MPIN must be 4-6 digits numeric";
    public static final String CONFIRM_MPIN_INVALID_PATTERN = "Confirm MPIN must be 4-6 digits numeric";


    // Login
    public static final String MPIN_INVALID = "Invalid MPIN";
    public static final String LOGIN_SUCCESS = "Login successful";

    // Device info validation messages
    public static final String IP_BLANK = "IP cannot be blank";
    public static final String DEVICE_ID_BLANK = "Device ID cannot be blank";
    public static final String LATITUDE_BLANK = "Latitude cannot be blank";
    public static final String LONGITUDE_BLANK = "Longitude cannot be blank";
    public static final String LOCATION_INVALID = "Invalid Latitude or Longitude value";


    // ---------- IP ----------
    public static final String IP_INVALID = "Invalid IP format";

    // ---------- Device ID ----------
    public static final String DEVICE_ID_INVALID = "Invalid device ID format";


    public static final String JWT_INVALID = "Invalid or expired JWT token.";

    public static final String ACCESS_DENIED = "You cannot access another customer's data.";

    public static final String INVALID_REFRESH_TOKEN = "Invalid refresh token";
    public static final String REFRESH_TOKEN_EXPIRED = "Refresh token expired";
    public static final String LOGOUT_SUCCESS = "User logged out successfully";

    // Forget MPIN
    public static final String MPIN_RESET_SUCCESS = "MPIN reset successfully. Please login with new MPIN.";
    public static final String MPIN_MISMATCH = "MPIN not match to confirmMpin";

    public static final String INVALID_DATE_RANGE = "Invalid date range";

    //For Card
    public static final String CARD_ID_BLANK = "Card number cannot be blank";
    public static final String CARD_ID_INVALID = "Card number must be 16 digits and pass Luhn check";
    public static final String HOLDER_NAME_BLANK = "Card holder name cannot be blank";
    public static final String HOLDER_NAME_INVALID = "Card holder name must be 2-50 chars, letters and spaces only";
    public static final String VALID_THRU_BLANK = "Expiry date cannot be blank";
    public static final String VALID_THRU_INVALID = "Expiry must be in MM/yy format and a future date";
    public static final String CARD_ALREADY_EXISTS = "Card already exists for this user";
    public static final String OTP_FORMAT_INVALID = "OTP must be 6 digits";
    public static final String CARD_NOT_FOUND = "Card not found or not owned by user";
    public static final String TOO_MANY_OTP_ATTEMPTS = "Too many invalid OTP attempts. Try again later.";

    // Bank Transfer
    public static final String ACCOUNT_BLANK = "Account number cannot be blank";
    public static final String IFSC_BLANK = "IFSC code cannot be blank";
    public static final String PAYEE_NAME_BLANK = "Payee name cannot be blank";
    public static final String AMOUNT_INVALID = "Amount must be greater than 0";

    public static final String TRANSFER_SUCCESS = "Fund transfer completed successfully";
    ;


    public static final String ACCOUNT_LINKED = "Account linked successfully";
    public static final String ACCOUNT_FETCHED = "Fetched account details successfully";
    public static final String ACCOUNTS_FETCHED = "Fetched all linked accounts successfully";
    public static final String BALANCE_FETCHED = "Fetched account balance successfully";
//    public static final String ACCOUNT_NOT_FOUND = "Account not found";
//    public static final String ACCOUNT_ALREADY_EXISTS = "Account already linked with this user";
//    public static final String ACCOUNT_NUMBER_BLANK = "Account number cannot be blank";





}
