package com.securebank.fraud;
import com.securebank.model.Account;
import com.securebank.model.Transaction;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public class RapidTransactionRule implements FraudRule {

    private final int windowSeconds;
    private final int maxTransactionsInWindow;

    public RapidTransactionRule(int windowSeconds, int maxTransactionsInWindow) {
        this.windowSeconds = windowSeconds;
        this.maxTransactionsInWindow = maxTransactionsInWindow;
    }

    @Override
    public boolean isSuspicious(Transaction transaction, Account sourceAccount) {
        List<Transaction> history = sourceAccount.getHistory();
        LocalDateTime cutoff = transaction.getTimestamp().minusSeconds(windowSeconds);

        long recentCount = history.stream()
                .filter(t -> t.getTimestamp().isAfter(cutoff))
                .count();

        return recentCount >= maxTransactionsInWindow;
    }

    @Override
    public String getDescription() {
        return maxTransactionsInWindow + "+ transactions within " + windowSeconds + " seconds";
    }
}
