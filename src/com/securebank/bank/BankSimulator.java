package com.securebank.bank;

import com.securebank.exceptions.*;
import com.securebank.fraud.FraudDetectionEngine;
import com.securebank.model.Account;
import com.securebank.model.OTP;
import com.securebank.model.Transaction;
import com.securebank.otp.OTPManager;
import com.securebank.persistence.AuditDAO;
import com.securebank.util.BankLogger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BankSimulator {

    private final Map<String, Account> accounts = new ConcurrentHashMap<>();
    private final Map<String, Transaction> pendingTransactions = new ConcurrentHashMap<>();

    private final OTPManager otpManager = new OTPManager();
    private final AuditDAO auditDAO = new AuditDAO();
    private final FraudDetectionEngine fraudEngine;

    public BankSimulator() {
        this.fraudEngine = new FraudDetectionEngine(accounts, auditDAO);
        this.fraudEngine.start(); // background thread begins listening immediately
    }

    public void registerAccount(Account account) {
        accounts.put(account.getAccountNumber(), account);
        BankLogger.info("Registered " + account);
    }

    public Account getAccount(String accountNumber) {
        return accounts.get(accountNumber);
    }

    /**
     * Step 1 of a transfer: creates a PENDING_OTP transaction and issues
     * an OTP for it. Nothing moves yet -- funds only move after verifyAndComplete().
     */
    public Transaction initiateTransfer(String sourceAcc, String destAcc, double amount, String originDevice) {
        Transaction txn = new Transaction(sourceAcc, destAcc, amount, originDevice);
        pendingTransactions.put(txn.getTransactionId(), txn);
        OTP otp = otpManager.issueOTP(txn.getTransactionId());
        BankLogger.info("Transfer initiated: " + txn.getTransactionId() +
                " | OTP sent to " + accounts.get(sourceAcc).getOwner().getPhoneNumber());
        // In a real bank this OTP would be sent via SMS; we expose it here for the demo.
        return txn;
    }

    /**
     * Step 2: verifies the OTP, and if valid, atomically moves funds.
     * The completed transaction is then queued for fraud screening.
     */
    public void verifyAndComplete(String transactionId, String submittedOtp)
            throws InvalidOTPException, OTPExpiredException, OTPLockedException, InsufficientFundsException {

        Transaction txn = pendingTransactions.get(transactionId);
        if (txn == null) {
            throw new InvalidOTPException("No pending transaction with id " + transactionId);
        }

        otpManager.verify(transactionId, submittedOtp);
        txn.setStatus(Transaction.Status.OTP_VERIFIED);

        Account source = accounts.get(txn.getSourceAccount());
        Account dest = accounts.get(txn.getDestinationAccount());

        // Lock ordering by account number prevents deadlock if two transfers
        // between the same pair of accounts run concurrently in opposite directions.
        Account first = source.getAccountNumber().compareTo(dest.getAccountNumber()) < 0 ? source : dest;
        Account second = first == source ? dest : source;

        synchronized (first) {
            synchronized (second) {
                // Captured *before* withdraw() so this transaction can't inflate
                // its own baseline and dodge HighAmountRule (see FraudRule).
                txn.setPriorAverageAmount(source.getAverageTransactionAmount());
                source.withdraw(txn.getAmount());
                dest.deposit(txn.getAmount());
            }
        }

        source.addToHistory(txn);
        txn.setStatus(Transaction.Status.COMPLETED);
        pendingTransactions.remove(transactionId);

        auditDAO.saveTransaction(txn);
        fraudEngine.submit(txn); // hand off to background fraud screening thread

        BankLogger.info("Transfer completed: " + txn);
    }

    public void shutdown() {
        otpManager.shutdown();
        fraudEngine.shutdown();
    }

    /**
     * DEMO-ONLY: stands in for the SMS/push channel that would deliver the
     * OTP to the user's phone in a real deployment. See OTPManager.peekCodeForDemo().
     */
    public String peekOtpForDemo(String transactionId) {
        return otpManager.peekCodeForDemo(transactionId);
    }
}
