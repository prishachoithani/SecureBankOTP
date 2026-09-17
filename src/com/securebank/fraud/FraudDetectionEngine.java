package com.securebank.fraud;

import com.securebank.model.Account;
import com.securebank.model.Transaction;
import com.securebank.persistence.AuditDAO;
import com.securebank.util.BankLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Runs as its own daemon thread and continuously pulls completed
 * transactions off a BlockingQueue (producer/consumer pattern), running
 * every registered FraudRule against each one. This decouples fraud
 * screening from the main transaction path so screening never blocks
 * a legitimate transfer from completing.
 */
public class FraudDetectionEngine extends Thread {

    private final BlockingQueue<Transaction> incomingTransactions = new LinkedBlockingQueue<>();
    private final List<FraudRule> rules = new ArrayList<>();
    private final Map<String, Account> accountRegistry;
    private final AuditDAO auditDAO;
    private volatile boolean running = true;

    public FraudDetectionEngine(Map<String, Account> accountRegistry, AuditDAO auditDAO) {
        super("Fraud-Detection-Thread");
        setDaemon(true);
        this.accountRegistry = accountRegistry;
        this.auditDAO = auditDAO;

        // Default rule set -- easy to extend with more FraudRule implementations.
        rules.add(new HighAmountRule(3.0));
        rules.add(new RapidTransactionRule(20, 3));
    }

    /** Called by the transaction pipeline to submit a completed transaction for screening. */
    public void submit(Transaction transaction) {
        incomingTransactions.offer(transaction);
    }

    public boolean isFlagged(Transaction transaction, Account sourceAccount) {
        return rules.stream().anyMatch(rule -> rule.isSuspicious(transaction, sourceAccount));
    }

    @Override
    public void run() {
        while (running) {
            try {
                Transaction txn = incomingTransactions.take(); // blocks until a txn arrives
                screen(txn);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                running = false;
            }
        }
    }

    private void screen(Transaction txn) {
        Account source = accountRegistry.get(txn.getSourceAccount());
        if (source == null) {
            return;
        }

        for (FraudRule rule : rules) {
            if (rule.isSuspicious(txn, source)) {
                FraudAlert alert = new FraudAlert(txn.getTransactionId(), rule.getDescription());
                BankLogger.alert("FRAUD FLAG on " + txn.getTransactionId() +
                        " -- " + rule.getDescription());
                auditDAO.saveAlert(alert);
            }
        }
    }

    public void shutdown() {
        running = false;
        this.interrupt();
    }
}
