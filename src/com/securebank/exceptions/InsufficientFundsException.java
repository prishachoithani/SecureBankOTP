package com.securebank.exceptions;

/** Thrown when a withdrawal/transfer exceeds the available account balance. */
public class InsufficientFundsException extends Exception {
    public InsufficientFundsException(String message) {
        super(message);
    }
}
