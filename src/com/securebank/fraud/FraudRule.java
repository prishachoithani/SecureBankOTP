package com.securebank.fraud;

import com.securebank.model.Account;
import com.securebank.model.Transaction;

public interface FraudRule {
    boolean isSuspicious(Transaction transaction, Account sourceAccount);
    String getDescription();
}
