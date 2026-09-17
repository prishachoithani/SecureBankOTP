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

    private void expireIfUnconsumed(OTP otp) {
        if (!otp.isConsumed()) {
            otp.markExpired();
            activeOtps.remove(otp.getTransactionId());
            BankLogger.info("OTP for txn " + otp.getTransactionId() + " expired automatically.");
        }
    }

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
            throw new InvalidOTPException("OTP for transaction " + transactionId + " was already used.");
        }

        activeOtps.remove(transactionId);
        BankLogger.info("OTP verified successfully for txn " + transactionId);
    }

    public void shutdown() {
        expiryScheduler.shutdownNow();
    }

    public String peekCodeForDemo(String transactionId) {
        OTP otp = activeOtps.get(transactionId);
        return otp == null ? null : otp.getCode();
    }
}
