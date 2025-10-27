package com.banking.semba.constants;

public class LogMessages {

    public static final String LIVE_BALANCE_FETCHED_SUCCESSFULLY = "Live balance fetched successfully: {}";

    private LogMessages() {
    } // prevent instantiation

    public static final String OTP_VERIFY_REQUEST_RECEIVED = "OTP verify request received for mobile: {}";
    public static final String SIGNUP_REQUEST_RECEIVED = "{\"event\":\"signup_request\",\"mobile\":\"{}\"}";
    public static final String MOBILE_BLANK = "{\"event\":\"mobile_blank\",\"mobile\":\"{}\"}";
    public static final String MOBILE_INVALID_PATTERN = "{\"event\":\"invalid_mobile_pattern\",\"mobile\":\"{}\"}";
    public static final String SIGNUP_REQUEST_UNEXCEPTED =  "{\"event\":\"Unexpected error during signupStart for mobile\",\"mobile\":\"{}\",\"error\":\"{}\"}";
    public static final String MOBILE_ALREADY_REGISTERED = "{\"event\":\"mobile_already_registered\",\"mobile\":\"{}\"}";
    public static final String USER_SAVED = "{\"event\":\"user_saved\",\"mobile\":\"{}\"}";
    public static final String CALLING_BANK_API = "{\"event\":\"calling_bank_api\",\"mobile\":\"{}\"}";
    public static final String OTP_FAILED = "{\"event\":\"otp_failed\",\"mobile\":\"{}\"}";
    public static final String OTP_SUCCESS = "{\"event\":\"otp_sent_success\",\"mobile\":\"{}\"}";


    //Verify OTP
    public static final String USER_NOT_FOUND = "{\"event\":\"user_not_found\",\"mobile\":\"{}\"}";
    public static final String OTP_BLANK = "{\"event\":\"otp_blank\",\"mobile\":\"{}\"}";
    public static final String OTP_INVALID = "{\"event\":\"otp_invalid\",\"mobile\":\"{}\"}";
    public static final String USER_OTP_VERIFIED = "{\"event\":\"otp_verified_success\",\"mobile\":\"{}\"}";
    public static final String OTP_VERIFIED_SUCCESS = "{\"event\":\"otp_verified_success\",\"mobile\":\"{}\"}";

    //Resend OTP
    public static final String MOBILE_ALREADY_VERIFIED = "{\"event\":\"mobile_already_verified\",\"mobile\":\"{}\"}";
    public static final String OTP_RESENT = "{\"event\":\"otp_resent\",\"mobile\":\"{}\"}";
    public static final String OTP_RESENT_SUCCESS = "{\"event\":\"otp_resent_success\",\"mobile\":\"{}\"}";

    //referral Code
    public static final String REFERRAL_CODE_SAVED = "{\"event\":\"referral_code_saved\",\"referralCode\":\"{}\",\"mobile\":\"{}\"}";

    // MPIN related
    public static final String SET_MPIN_REQUEST_RECEIVED = "{\"event\":\"set_mpin_request\",\"mobile\":\"{}\"}";
    public static final String MPIN_DOES_NOT_MATCH = "{\"event\":\"mpin_not_match\",\"mobile\":\"{}\"}";
    public static final String MPIN_INVALID_PATTERN = "{\"event\":\"mpin_invalid_pattern\",\"mobile\":\"{}\"}";
    public static final String CONFIRM_MPIN_INVALID_PATTERN = "{\"event\":\"confirm_mpin_invalid_pattern\",\"mobile\":\"{}\"}";
    public static final String MPIN_ERROR = "{\"event\":\"Unexpected error during MPIN set for\",\"mobile\":\"{}\",\"mobile\":\"{}\"}";

    ;
    public static final String CONFIRM_MPIN_BLANK = "{\"event\":\"confirm_mpin_blank\",\"mobile\":\"{}\"}";
    public static final String MPIN_SET_SUCCESS = "{\"event\":\"mpin_set_success\",\"mobile\":\"{}\"}";
    public static final String MPIN_BLANK = "{\"event\":\"mpin_blank\",\"mobile\":\"{}\"}";
    public static final String MPIN_INVALID = "{\"event\":\"mpin_invalid\",\"mobile\":\"{}\"}";
    public static final String MPIN_NOT_MATCH = "{\"event\":\"mpin_not_match\",\"mobile\":\"{}\"}";

    //login
    public static final String LOGIN_REQUEST = "{\"event\":\"login_request\",\"mobile\":\"{}\"}";
    public static final String LOGIN_SUCCESS = "{\"event\":\"login_success\",\"mobile\":\"{}\"}";

    // Device info logs
    public static final String IP_BLANK = "{\"event\":\"ip_blank\",\"mobile\":\"{}\"}";
    public static final String DEVICE_ID_BLANK = "{\"event\":\"device_id_blank\",\"mobile\":\"{}\"}";
    public static final String LATITUDE_BLANK = "{\"event\":\"latitude_blank\",\"mobile\":\"{}\"}";
    public static final String LONGITUDE_BLANK = "{\"event\":\"longitude_blank\",\"mobile\":\"{}\"}";
    public static final String LOCATION_INVALID = "{\"event\":\"location_invalid\",\"mobile\":\"{}\"}";


    // ---------- IP ----------
    public static final String IP_INVALID = "{\"event\":\"ip_invalid\",\"mobile\":\"{}\"}";

    // ---------- Device ID ----------
    public static final String DEVICE_ID_INVALID = "{\"event\":\"device_id_invalid\",\"mobile\":\"{}\"}";

    // ---------- Location ----------
//    public static final String LOCATION_INVALID = "{\"event\":\"location_invalid\",\"mobile\":\"{}\"}";

    public static final String JWT_INVALID = "{\"event\":\"jwt_invalid\",\"reason\":\"{}\"}";

    // Profile
    public static final String PROFILE_FETCH_START = "Fetching profile for mobile {}";
    public static final String PROFILE_FETCH_SUCCESS = "Profile fetch success for mobile {}";
    public static final String PROFILE_FETCH_FAILED = "Profile fetch failed for mobile {}: {}";

    public static final String ACCOUNT_FETCH_START = "Fetching account {}";
    public static final String ACCOUNT_FETCH_SUCCESS = "Account fetch success for id {}";
    public static final String ACCOUNT_FETCH_FAILED = "Account fetch failed for id {}: {}";

    public static final String BANK_API_ERROR = "Bank API error [{}]: {}";

    // ---------- Refresh Token ----------
    public static final String REFRESH_TOKEN_REQUEST = "Refresh token request received | deviceId={} | ip={}";
    public static final String REFRESH_TOKEN_SUCCESS = "Refresh token successful for mobile={}";
    public static final String REFRESH_TOKEN_FAILED = "Refresh token failed: {}";
    public static final String REFRESH_TOKEN_EXPIRED = "Refresh token expired for mobile={}";
    public static final String REFRESH_TOKEN_CREATED = "Refresh token created for mobile: {}, deviceId: {}, IP: {}";

    public static final String LOGOUT_SUCCESS = "Logout successful for user: {}";
    public static final String LOGOUT_FAILED = "Logout failed: {}";


    // Forget MPIN
    public static final String FORGET_MPIN_REQUEST = "Forget MPIN request received for mobile: {}";
    public static final String FORGET_MPIN_SUCCESS = "Forget MPIN success for mobile: {}";
    public static final String MPIN_MISMATCH = "MPIN not match to confirmMpin: {}";

    public static final String GET_ACCOUNTS_SUCCESS = "Accounts list fetched successfully for mobile: {}";
    //    public static final String GET_ACCOUNTS_SUCCESS = "Accounts list fetched successfully for mobile: {}";
//    public static final String GET_ACCOUNT_SUCCESS = "Account details fetched successfully for mobile: {}";
    public static final String ACCOUNT_STATEMENT_FETCHED = "Account statement fetched for accountId: {} and mobile: {}";
//    public static final String BANK_API_ERROR = "Bank API error [{}]: {}";;


    //Card
    public static final String CARD_ADD_REQUEST = "CARD_ADD_REQUEST mobile={} maskedPan={}";
    public static final String CARD_ADD_SUCCESS = "CARD_ADD_SUCCESS mobile={} cardId={}";
    public static final String CARD_ADD_FAILED = "CARD_ADD_FAILED mobile={} reason={}";

    public static final String OTP_SENT = "OTP_SENT mobile={} maskedPan={}";
    public static final String OTP_VERIFY_REQUEST = "OTP_VERIFY_REQUEST mobile={} maskedPan={}";
    public static final String OTP_VERIFY_SUCCESS = "OTP_VERIFY_SUCCESS mobile={} maskedPan={}";
    public static final String OTP_VERIFY_FAILED = "Unexpected error during OTP verification for mobile {}: {}";

    public static final String DEVICE_VALIDATION_FAILED = "DEVICE_VALIDATION_FAILED mobile={} reason={}";
    public static final String GET_CARDS = "GET_CARDS mobile={} count={}";
    public static final String PAYMENT_REQUEST = "Payment request received for mobile: {}, card: {}";
    public static final String PAYMENT_SUCCESS = "Payment successful for mobile: {}, card: {}";

    //Bank transfer
    public static final String TRANSFER_REQUEST = "Transfer requested by user {} from {} to {}";
    public static final String TRANSFER_SUCCESS = "Transfer SUCCESS: {} -> {} Amount: {}";
    public static final String TRANSFER_FAILED = "Transfer FAILED: {} -> {} Amount: {} Reason: {}";
    public static final String OTP_VERIFICATION = "OTP verification for user {} account {}";
    public static final String ACCESS_DENIED = "{\"event\":\"access_denied\",\"mobile\":\"{}\"}";
    public static final String GET_ACCOUNT_SUCCESS = "{\"event\":\"get_account_success\",\"mobile\":\"{}\"}";
    public static final String ACCOUNT_IS_ALREADY_EXISTS = "{\"event\":\"account_number\",\"accountNumber\":\"{}\"}";
    public static final String FETCH_BANKS_STARTED = "{\"event\":\"fetch_banks_started\",\"message\":\"Fetching bank list from external API\"}";
    public static final String FETCH_BANKS_SUCCESS = "{\"event\":\"fetch_banks_success\",\"message\":\"Successfully fetched bank list from external API\"}";
    public static final String FETCH_BANKS_NULL = "{\"event\":\"fetch_banks_null_response\",\"message\":\"Bank list API returned null or empty\"}";
    public static final String FETCH_BANKS_ERROR = "{\"event\":\"fetch_banks_error\",\"error\":\"{}\"}";
    public static final String FETCH_BANKS_UNEXPECTED_ERROR = "{\"event\":\"fetch_banks_unexpected_error\",\"error\":\"{}\"}";

    public static final String API_CALL = "{\"event\":\"external_api_call\",\"url\":\"{}\"}";
    public static final String API_RESPONSE = "{\"event\":\"external_api_response\",\"status\":\"{}\"}";

    // ---------- Refresh Token ----------
    public static final String SEARCH_BANKS_STARTED = "{\"event\":\"search_banks_started\",\"message\":\"Search banks started\"}";
    public static final String SEARCH_BANKS_SUCCESS = "{\"event\":\"search_banks_success\",\"message\":\"Searched banks successfully!\"}";
    public static final String SEARCH_CONTACTS_ERROR = "{\"event\":\"search_contacts_error\",\"error\":\"{}\"}";
    public static final String SEARCH_CONTACTS_START = "{\"event\":\"search_contacts_start\",\"mobile\":\"{}\",\"name\":\"{}\"}";
    public static final String SEARCH_CONTACTS_SUCCESS = "{\"event\":\"search_contacts_success\",\"count\":{}}";

    public static final String UPIID_VALIDATION_START = "{\"event\":\"upiid_validation_start\",\"upiId\":\"{}\"}";
    public static final String UPIID_VALIDATION_UNAUTHORIZED = "{\"event\":\"upiid_validation_unauthorized\",\"message\":\"{}\"}";
    public static final String UPIID_VALIDATION_ERROR = "{\"event\":\"upiid_validation_error\",\"message\":\"{}\"}";
    public static final String UPIID_VALIDATION_NOT_FOUND = "{\"event\":\"upiid_validation_not_found\",\"upiId\":\"{}\"}";
    public static final String UPIID_VALIDATION_SUCCESS = "{\"event\":\"upiid_validation_success\",\"upiId\":\"{}\",\"name\":\"{}\"}";


}

