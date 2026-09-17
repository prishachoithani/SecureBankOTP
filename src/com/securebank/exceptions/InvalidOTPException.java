package com.securebank.exceptions;
public class InvalidOTPException extends Exception {
    public InvalidOTPException(String message) {
        super(message);
    }
}
