package com.securebank.fraud;

import com.securebank.model.Account;
import com.securebank.model.Transaction;

/**
 * Strategy-pattern interface: each concrete rule inspects a transaction
 * (plus the source account's history) and decides whether it looks
 * fraudulent. The FraudDetectionEngine applies every registered rule
 * to each transaction it consumes from its queue.
 */
public interface FraudRule {

    /** @return true if this rule considers the transaction suspicious. */
    boolean isSuspicious(Transaction transaction, Account sourceAccount);

    /** Human-readable reason, used in alert logs. */
    String getDescription();
}
