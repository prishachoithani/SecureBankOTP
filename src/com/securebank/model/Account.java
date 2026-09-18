package com.securebank.model;
import com.securebank.exceptions.InsufficientFundsException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Account {
    private final String accountNumber;
    private final User owner;
    private double balance;
    private final List<Transaction> history;

    private double averageTransactionAmount = 0.0;
    private int transactionCount = 0;

    public Account(String accountNumber, User owner, double openingBalance) {
        this.accountNumber = accountNumber;
        this.owner = owner;
        this.balance = openingBalance;
        this.history = Collections.synchronizedList(new ArrayList<>());
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public User getOwner() {
        return owner;
    }

    public synchronized double getBalance() {
        return balance;
    }

    public synchronized double getAverageTransactionAmount() {
        return averageTransactionAmount;
    }

    public synchronized void withdraw(double amount) throws InsufficientFundsException {
        if (amount > balance) {
            throw new InsufficientFundsException(
                    "Insufficient funds in account " + accountNumber +
                    ": balance=" + balance + ", requested=" + amount);
        }
        balance -= amount;
        updateRunningAverage(amount);
    }

    public synchronized void deposit(double amount) {
        balance += amount;
        updateRunningAverage(amount);
    }

    private void updateRunningAverage(double amount) {
        transactionCount++;
        averageTransactionAmount =
                ((averageTransactionAmount * (transactionCount - 1)) + amount) / transactionCount;
    }

    public void addToHistory(Transaction t) {
        history.add(t);
    }

    public List<Transaction> getHistory() {
        synchronized (history) {
            return new ArrayList<>(history);
        }
    }

    @Override
    public String toString() {
        return "Account{number='" + accountNumber + "', owner=" + owner.getName() +
                ", balance=" + balance + "}";
    }
}
