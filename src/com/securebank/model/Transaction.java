package com.securebank.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Immutable record of a single money transfer attempt.
 * 'status' is updated as the transaction moves through OTP verification
 * and fraud screening, so the object doubles as a small state machine.
 */
public class Transaction {

    public enum Status {
        PENDING_OTP, OTP_VERIFIED, FLAGGED_FRAUD, COMPLETED, FAILED
    }

    private final String transactionId;
    private final String sourceAccount;
    private final String destinationAccount;
    private final double amount;
    private final String originDevice;
    private final LocalDateTime timestamp;
    private Status status;
    private double priorAverageAmount = 0.0; // account's avg txn size *before* this one — used by fraud rules

    public Transaction(String sourceAccount, String destinationAccount, double amount, String originDevice) {
        this.transactionId = UUID.randomUUID().toString().substring(0, 8);
        this.sourceAccount = sourceAccount;
        this.destinationAccount = destinationAccount;
        this.amount = amount;
        this.originDevice = originDevice;
        this.timestamp = LocalDateTime.now();
        this.status = Status.PENDING_OTP;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getSourceAccount() {
        return sourceAccount;
    }

    public String getDestinationAccount() {
        return destinationAccount;
    }

    public double getAmount() {
        return amount;
    }

    public String getOriginDevice() {
        return originDevice;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public synchronized Status getStatus() {
        return status;
    }

    public synchronized void setStatus(Status status) {
        this.status = status;
    }

    /**
     * The source account's average transaction amount *before* this
     * transaction was applied. Captured at withdrawal time so fraud
     * rules can judge "is this unusually large?" against the account's
     * prior behavior, without the current (possibly fraudulent)
     * transaction skewing its own baseline.
     */
    public double getPriorAverageAmount() {
        return priorAverageAmount;
    }

    public void setPriorAverageAmount(double priorAverageAmount) {
        this.priorAverageAmount = priorAverageAmount;
    }

    @Override
    public String toString() {
        return String.format("Txn[%s] %s -> %s | amount=%.2f | status=%s | at=%s",
                transactionId, sourceAccount, destinationAccount, amount, status, timestamp);
    }
}
