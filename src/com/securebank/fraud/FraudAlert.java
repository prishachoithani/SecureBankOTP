package com.securebank.fraud;

import java.time.LocalDateTime;

public class FraudAlert {
    private final String transactionId;
    private final String ruleTriggered;
    private final LocalDateTime raisedAt;
    public FraudAlert(String transactionId, String ruleTriggered) {
        this.transactionId = transactionId;
        this.ruleTriggered = ruleTriggered;
        this.raisedAt = LocalDateTime.now();
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getRuleTriggered() {
        return ruleTriggered;
    }

    public LocalDateTime getRaisedAt() {
        return raisedAt;
    }

    @Override
    public String toString() {
        return "FraudAlert[txn=" + transactionId + ", rule='" + ruleTriggered + "', at=" + raisedAt + "]";
    }
}
