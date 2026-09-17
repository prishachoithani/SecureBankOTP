package com.securebank.exceptions;

/** Thrown when the fraud detection engine blocks a transaction before it is completed. */
public class FraudDetectedException extends Exception {
    public FraudDetectedException(String message) {
        super(message);
    }
}
