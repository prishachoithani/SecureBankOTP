package com.securebank.otp;

import com.securebank.exceptions.InvalidOTPException;
import com.securebank.exceptions.OTPExpiredException;
import com.securebank.exceptions.OTPLockedException;
import com.securebank.model.OTP;
import com.securebank.util.BankLogger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Issues, tracks and expires OTPs for pending transactions.
 *
 * Each OTP is scheduled to auto-expire on a background thread pool
 * (ScheduledExecutorService) after its validity window -- this is the
 * multithreading requirement in action: expiry does not depend on the
 * user or the main thread polling anything.
 *
 * verify() is synchronized per-OTP via OTP.tryConsume(), so a real-world
 * attack pattern -- an attacker and the legitimate user both racing to
 * submit the same intercepted OTP -- cannot result in the OTP being
 * accepted twice.
 */
public class OTPManager {

    private static final int DEFAULT_VALIDITY_SECONDS = 30;
    private static final int DEFAULT_MAX_ATTEMPTS = 3;

    private final Map<String, OTP> activeOtps = new ConcurrentHashMap<>();
    private final ScheduledExecutorService expiryScheduler =
            Executors.newScheduledThreadPool(2, r -> {
                Thread t = new Thread(r, "OTP-Expiry-Thread");
                t.setDaemon(true);
                return t;
            });

    /** Generates a new OTP for a transaction and schedules its auto-expiry. */
    public OTP issueOTP(String transactionId) {
        OTP otp = new OTP(transactionId, DEFAULT_VALIDITY_SECONDS, DEFAULT_MAX_ATTEMPTS);
        activeOtps.put(transactionId, otp);

        expiryScheduler.schedule(
                () -> expireIfUnconsumed(otp),
                otp.getValiditySeconds(),
                TimeUnit.SECONDS
        );

        BankLogger.info("OTP issued for txn " + transactionId +
                " (valid " + otp.getValiditySeconds() + "s): " + otp.getCode());
        return otp;
    }

    /**
     * Runs on a background thread once an OTP's validity window elapses.
     * Marks it expired and removes it from the active map so a stale
     * code can never be verified afterwards.
     */
    private void expireIfUnconsumed(OTP otp) {
        if (!otp.isConsumed()) {
            otp.markExpired();
            activeOtps.remove(otp.getTransactionId());
            BankLogger.info("OTP for txn " + otp.getTransactionId() + " expired automatically.");
        }
    }

    /**
     * Verifies a submitted OTP code against the one on record.
     * Throws distinct exception types so callers (and log output) can tell
     * apart "wrong code", "expired", and "too many attempts" failures.
     */
    public void verify(String transactionId, String submittedCode)
            throws InvalidOTPException, OTPExpiredException, OTPLockedException {

        OTP otp = activeOtps.get(transactionId);
        if (otp == null) {
            throw new InvalidOTPException("No active OTP found for transaction " + transactionId);
        }

        if (otp.isExpired()) {
            throw new OTPExpiredException("OTP for transaction " + transactionId + " has expired.");
        }

        if (otp.getAttemptsRemaining() <= 0) {
            throw new OTPLockedException("Too many failed OTP attempts for transaction " + transactionId);
        }

        if (!otp.getCode().equals(submittedCode)) {
            int remaining = otp.decrementAndGetAttempts();
            throw new InvalidOTPException(
                    "Incorrect OTP for transaction " + transactionId +
                    ". Attempts remaining: " + remaining);
        }

        if (!otp.tryConsume()) {
            // Someone else consumed it in the instant between our checks above.
            throw new InvalidOTPException("OTP for transaction " + transactionId + " was already used.");
        }

        activeOtps.remove(transactionId);
        BankLogger.info("OTP verified successfully for txn " + transactionId);
    }

    public void shutdown() {
        expiryScheduler.shutdownNow();
    }

    /**
     * DEMO-ONLY hook: exposes the current OTP code for a transaction so
     * this project can be run end-to-end from a single console app without
     * a real SMS gateway. A production system would never expose this --
     * the code would only ever leave the server via the SMS/push channel.
     */
    public String peekCodeForDemo(String transactionId) {
        OTP otp = activeOtps.get(transactionId);
        return otp == null ? null : otp.getCode();
    }
}
