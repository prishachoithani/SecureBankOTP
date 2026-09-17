package com.securebank.exceptions;

/** Thrown when a user attempts to verify an OTP after its validity window has elapsed. */
public class OTPExpiredException extends Exception {
    public OTPExpiredException(String message) {
        super(message);
    }
}
