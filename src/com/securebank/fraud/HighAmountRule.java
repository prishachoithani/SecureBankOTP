package com.securebank.fraud;

import com.securebank.model.Account;
import com.securebank.model.Transaction;

/**
 * Flags a transaction if its amount is far above the account's
 * historical average -- a classic signal of an OTP-fraud attacker
 * trying to drain an account in one large transfer.
 */
public class HighAmountRule implements FraudRule {

    private final double multiplierThreshold;

    public HighAmountRule(double multiplierThreshold) {
        this.multiplierThreshold = multiplierThreshold;
    }

    @Override
    public boolean isSuspicious(Transaction transaction, Account sourceAccount) {
        // Uses the average captured *before* this transaction was applied,
        // not the account's live average -- otherwise a single large
        // fraudulent transfer would inflate its own baseline and never
        // trip this rule (a bug caught during testing).
        double avg = transaction.getPriorAverageAmount();
        if (avg <= 0) {
            return false; // not enough history yet to judge
        }
        return transaction.getAmount() > avg * multiplierThreshold;
    }

    @Override
    public String getDescription() {
        return "Transaction amount exceeds " + multiplierThreshold +
                "x the account's average transaction size";
    }
}
