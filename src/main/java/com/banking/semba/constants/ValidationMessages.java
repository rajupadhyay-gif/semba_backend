package com.banking.semba.constants;

public class ValidationMessages {

    public static final String INSUFFICIENT_FUNDS = "Insufficient balance. Available: ₹";
    public static final String SUFFICIENT_FUNDS = "Sufficient balance available. Transaction allowed.";

    private ValidationMessages() {
    }

    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";
    public static final String BAD_REQUEST = "BAD_REQUEST";
    public static final String FAILURE = "Filed to fetch the payees list";
    public static final String MSG_PAYEE_DELETED_SUCCESS = "Payee Deleted Successfully";
    public static final String CLIENT_ERROR = "Client error while deleting payee";
    public static final String SERVER_ERROR = "Server error while deleting payee";
    public static final Object DELETED_PAYEE = "Deleted beneficiary with ID: ";
    public static final String FAILED_TO_DELETE = "Deleted beneficiary with ID: ";
    public static final String ERROR_CODE_FETCH_FAILED = "501";
    public static final String ERROR_CODE_NO_BANKS = "404";
    public static final String STATUS_FAILED = "Failed";
    public static final String TRANSACTION_NOT_ALLOWED = "Transaction declined";

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
    public static final String CARD_ADDED_SUCCESS = "Card add Successfully";
    public static final String CARD_ADD_FAILED = "Card added failed";
    public static final String CARD_OTP_VERIFY_SUCCESS = "Card otp verified successfully";
    public static final String CARDS_LIST_FETCH_SUCCESS = "Cards list fetched successfully";
    public static final String CARDS_FETCH_FAILED = "Cards list fetched failed";
    public static final String PAYMENT_SUCCESS = "Payment successful";



    // Bank Transfer
    public static final String ACCOUNT_BLANK = "Account number cannot be blank";
    public static final String IFSC_BLANK = "IFSC code cannot be blank";
    public static final String PAYEE_NAME_BLANK = "Payee name cannot be blank";
    public static final String AMOUNT_INVALID = "Amount must be greater than 0";

    public static final String TRANSFER_SUCCESS = "Fund transfer completed successfully";
    ;


    public static final String ACCOUNT_FETCH_SUCCESS = "Account details fetched successfully.";
    public static final String ACCOUNT_FETCH_FAILED = "Failed to fetch account details.";
    public static final String BANK_API_FAILED = "Bank API call failed. Please try again later.";
    public static final String UNKNOWN_ERROR = "Something went wrong. Please try again later.";
    public static final String FUND_TRANSFER_ERROR = "Client error during fund transfer.";
    public static final String FUND_TRANSFER_SERVERERROR = "Fund transfer error during fund transfer.";
    public static final String FETCH_ACCOUNT_ERROR = "Client error while fetching account.";
    public static final String FETCH_SERVER_ERROR = "Server error while fetching account.";


//    public static final String ACCOUNT_NOT_FOUND = "Account not found";
//    public static final String ACCOUNT_ALREADY_EXISTS = "Account already linked with this user";
//    public static final String ACCOUNT_NUMBER_BLANK = "Account number cannot be blank";

    public static final String UPDATED_SUCCESSFULLY = "Payee updated successfully" ;
    public static final String FETCHED_SUCCESSFULLY = "Banks fetched successfully";
    public static final String NO_CONTACTS_FOUND= "No contacts found";
    public static final String CONTACTS_FETCHED_SUCCESSFULLY="Contacts fetched successfully";
    public static final String STATUS_ERROR = "Internal Server Error";
    public static final String NO_RECENT_PAYMENTS = "No payments found";
    public static final String SENDER_MOBILE_REQUIRED = "Sender mobile number is required";
    public static final String INVALID_MOBILE_FORMAT = "Invalid mobile number format";
    public static final String RECENT_PAYMENTS_FETCHED = "Recent payments fetched successfully";
    public static final String UPI_ID_VERIFIED_SUCCESSFULLY = "UPI Id verified Successfully";
    public static final String NO_UPIID_FOUND = "UPI Id Not Found";
    public static final String MSG_PAYEES_FETCHED = "Payees fetched successfully";
    public static final String SAME_NAME_UPDATE = "New beneficiary name is the same as the existing name";
    public static final String NOT_FOUND = "Not Found";
    public static final String EXTERNAL_API_ERROR = "External API error occurred";
    public static final String EXTERNAL_API_NO_RESPONSE = "No response from external API";
    public static final String UPDATE_FAILED = "Failed to update payee";
    public static final String INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR";
    public static final String BENEFICIARY_ADDED_SUCCESSFULLY = "Beneficiary Added Successfully";
    public static final String STATUS_OK = "SUCCESS";

    public static final String NO_BANKS_FOUND = "No banks found from external API";
    public static final String INVALID_JWT = "Invalid JWT token or mobile not found";
    public static final String NO_PAYEES_FOUND = "No payees found for this user";
    public static final String INVALID_BANK = "Invalid bank selected";
    public static final String BENEFICIARY_NAME_REQUIRED = "Beneficiary name is required";
    public static final String ACCOUNT_NUMBER_MISMATCH = "Account number and confirm account number do not match";
    public static final String INVALID_IFSC = "Invalid IFSC format";
    public static final String STATUS_UNAUTHORIZED = "UNAUTHORIZED";
    public static final String STATUS_FORBIDDEN = "FORBIDDEN";
    public static final String CONFLICT = "Conflict";
    public static final String BANK_ID_REQUIRED = "Please Enter Valid Bank ID";
    public static final String ACCOUNT_NUMBER_REQUIRED = "Please Enter Valid Account Number";

    public static final String FETCHING_FAILED="Failed to fetch banks list: ";
    public static final String PROFILE_FETCH_SUCCESS = "Profile fetched successfully";
    public static final String PROFILE_FETCH_FAILED = "Failed to fetch profile";
    public static final String ACCOUNT_FETCH_ERROR = "Failed to fetch account";
    public static final String INVALID_RESPONSE = "Invalid response from bank API";
//    public static final String UNKNOWN_ERROR = "Unexpected error occurred";
    public static final String TRANSACTION_ALLOWED = "Transaction permitted";
}
