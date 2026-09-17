package com.securebank.model;

import java.security.SecureRandom;
import java.time.LocalDateTime;

/**
 * A one-time password bound to a specific transaction.
 * Carries its own expiry timestamp; OTPManager runs a background
 * thread that invalidates it once the window elapses.
 */
public class OTP {
    private final String code;
    private final String transactionId;
    private final LocalDateTime issuedAt;
    private final int validitySeconds;
    private int attemptsRemaining;
    private volatile boolean expired = false;
    private volatile boolean consumed = false;

    private static final SecureRandom RNG = new SecureRandom();

    public OTP(String transactionId, int validitySeconds, int maxAttempts) {
        this.transactionId = transactionId;
        this.code = generateCode();
        this.issuedAt = LocalDateTime.now();
        this.validitySeconds = validitySeconds;
        this.attemptsRemaining = maxAttempts;
    }

    private String generateCode() {
        int number = 100000 + RNG.nextInt(900000); // 6-digit OTP
        return String.valueOf(number);
    }

    public String getCode() {
        return code;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public int getValiditySeconds() {
        return validitySeconds;
    }

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public synchronized boolean isExpired() {
        return expired;
    }

    public synchronized void markExpired() {
        this.expired = true;
    }

    public synchronized boolean isConsumed() {
        return consumed;
    }

    /**
     * Atomically consumes the OTP so it cannot be used twice, even if two
     * threads try to verify it at the same instant (protects against
     * OTP-replay style race conditions).
     */
    public synchronized boolean tryConsume() {
        if (consumed || expired) {
            return false;
        }
        consumed = true;
        return true;
    }

    public synchronized int decrementAndGetAttempts() {
        attemptsRemaining--;
        return attemptsRemaining;
    }

    public synchronized int getAttemptsRemaining() {
        return attemptsRemaining;
    }
}
