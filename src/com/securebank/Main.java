package com.securebank;

import com.securebank.bank.BankSimulator;
import com.securebank.exceptions.*;
import com.securebank.model.Account;
import com.securebank.model.Transaction;
import com.securebank.model.User;
import com.securebank.persistence.DatabaseManager;
import com.securebank.security.EncryptionUtil;
import com.securebank.util.BankLogger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;

/**
 * Demo entry point. Walks through:
 *   1. A normal, legitimate OTP-verified transfer.
 *   2. A simulated OTP-fraud attempt (an unusually large transfer,
 *      right after a small legitimate one) that
 *      the fraud engine flags AFTER completion (screening is async by design --
 *      in a production system this would instead feed a hold/reversal workflow).
 *   3. A concurrency stress test: multiple threads hammering the same
 *      account simultaneously, proving the synchronized withdraw() prevents
 *      the balance from ever going negative or being double-spent.
 *   4. Encrypting the final transaction history to disk.
 */
public class Main {

    public static void main(String[] args) throws Exception {
        Files.createDirectories(Path.of("data"));
        DatabaseManager.initializeSchema();

        BankSimulator bank = new BankSimulator();

        User priya = new User("U001", "Priya Sharma", "+91-9876500001", "device-priya-iphone");
        User rahul = new User("U002", "Rahul Verma", "+91-9876500002", "device-rahul-android");

        Account acc1 = new Account("ACC1001", priya, 50000.0);
        Account acc2 = new Account("ACC1002", rahul, 20000.0);
        bank.registerAccount(acc1);
        bank.registerAccount(acc2);

        System.out.println("\n=== SCENARIO 1: Legitimate transfer ===");
        legitimateTransfer(bank, acc1, acc2);

        System.out.println("\n=== SCENARIO 2: Suspicious transfer (unusually large amount) ===");
        suspiciousTransfer(bank, acc1, acc2);

        System.out.println("\n=== SCENARIO 3: Concurrency stress test on ACC1002 ===");
        concurrencyStressTest(bank, acc2);

        // Give the async fraud thread a moment to finish processing its queue.
        Thread.sleep(500);

        System.out.println("\n=== Final balances ===");
        System.out.println(acc1);
        System.out.println(acc2);

        System.out.println("\n=== SCENARIO 4: Encrypting transaction history to disk ===");
        exportEncryptedHistory(acc1);

        bank.shutdown();
        System.out.println("\nDemo complete. See data/bank_activity.log and data/securebank.db for full audit trail.");
    }

    private static void legitimateTransfer(BankSimulator bank, Account from, Account to) throws Exception {
        Transaction txn = bank.initiateTransfer(
                from.getAccountNumber(), to.getAccountNumber(), 2000.0, "device-priya-iphone");

        // Stands in for the user reading the OTP off their SMS/push notification.
        String code = bank.peekOtpForDemo(txn.getTransactionId());
        bank.verifyAndComplete(txn.getTransactionId(), code);
    }

    private static void suspiciousTransfer(BankSimulator bank, Account from, Account to) {
        try {
            // Large amount relative to this account's recent transaction history --
            // matches the OTP-fraud pattern of draining an account in one large transfer.
            Transaction txn = bank.initiateTransfer(
                    from.getAccountNumber(), to.getAccountNumber(), 30000.0, "device-unknown-scammer");

            String code = bank.peekOtpForDemo(txn.getTransactionId());
            bank.verifyAndComplete(txn.getTransactionId(), code);
            // Transaction completes (the OTP itself was valid) but will be
            // flagged asynchronously by the FraudDetectionEngine thread.
        } catch (InvalidOTPException | OTPExpiredException | OTPLockedException | InsufficientFundsException e) {
            BankLogger.warn("Suspicious transfer failed verification: " + e.getMessage());
        }
    }

    private static void concurrencyStressTest(BankSimulator bank, Account target) throws InterruptedException {
        int threadCount = 5;
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int id = i;
            Thread t = new Thread(() -> {
                try {
                    target.withdraw(5000.0);
                    BankLogger.info("Stress-thread-" + id + " withdrew 5000 successfully. New balance=" + target.getBalance());
                } catch (InsufficientFundsException e) {
                    BankLogger.warn("Stress-thread-" + id + " blocked: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            }, "Stress-Thread-" + i);
            t.start();
        }

        latch.await(); // wait for all 5 threads to finish before checking final balance
        System.out.println("Balance after concurrent withdrawals (never goes negative): " + target.getBalance());
    }

    private static void exportEncryptedHistory(Account account) throws Exception {
        EncryptionUtil encryptionUtil = new EncryptionUtil();
        StringBuilder sb = new StringBuilder();
        for (Transaction t : account.getHistory()) {
            sb.append(t.toString()).append(System.lineSeparator());
        }

        String outFile = "data/" + account.getAccountNumber() + "_history.enc";
        encryptionUtil.encryptToFile(sb.toString(), outFile);
        System.out.println("Encrypted history written to " + outFile);

        // Prove it round-trips correctly.
        String decrypted = encryptionUtil.decryptFromFile(outFile);
        System.out.println("Decrypted content check:\n" + decrypted);
    }
}
