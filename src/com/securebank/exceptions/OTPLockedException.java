package com.securebank.exceptions;

/** Thrown when too many incorrect OTP attempts have been made for a transaction. */
public class OTPLockedException extends Exception {
    public OTPLockedException(String message) {
        super(message);
    }
}
