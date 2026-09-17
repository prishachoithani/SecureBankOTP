package com.securebank.fraud;

import com.securebank.model.Account;
import com.securebank.model.Transaction;

public class HighAmountRule implements FraudRule {

    private final double multiplierThreshold;

    public HighAmountRule(double multiplierThreshold) {
        this.multiplierThreshold = multiplierThreshold;
    }

    @Override
    public boolean isSuspicious(Transaction transaction, Account sourceAccount) {
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
