package com.securebank.exceptions;

/** Thrown when a submitted OTP code does not match the one issued for a transaction. */
public class InvalidOTPException extends Exception {
    public InvalidOTPException(String message) {
        super(message);
    }
}
