package com.securebank.exceptions;
public class OTPLockedException extends Exception {
    public OTPLockedException(String message) {
        super(message);
    }
}
