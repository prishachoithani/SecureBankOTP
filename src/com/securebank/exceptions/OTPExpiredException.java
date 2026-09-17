package com.securebank.exceptions;
public class OTPExpiredException extends Exception {
    public OTPExpiredException(String message) {
        super(message);
    }
}
