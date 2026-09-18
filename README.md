# SecureBank OTP — Fraud-Resilient Transaction Simulator

A Java-based simulation of a bank's OTP-verified money transfer system with a
concurrent, rule-based fraud detection engine layered on top.

The project models the real-world problem of **OTP-based account-draining
fraud**, where a scammer may trick a victim into revealing a one-time password
and then attempt to use it to authorize an unauthorized transfer.



---

## Overview

SecureBank OTP simulates a banking transaction system where every transfer
must pass through two main stages before the transaction is completed.

### 1. OTP Verification

A 6-digit OTP is generated for every transaction.

The OTP:

- Is valid only for a limited amount of time.
- Can be used only once.
- Automatically expires in the background.
- Uses synchronized verification so that multiple threads cannot successfully
  consume the same OTP.

### 2. Fraud Detection

After a transaction is successfully completed, it is placed into a queue and
processed by a separate background fraud-detection thread.

The fraud detection engine checks transactions against implemented rules such
as:

- A transfer that is unusually large compared to the account's previous
  transaction history.
- Multiple transfers happening very quickly one after another.

If a transaction matches a fraud rule, an alert is generated and recorded in
the audit logs.

For the demonstration, the program first performs a normal transaction and
then performs a much larger transaction from the same account. This provides
a suspicious transaction for the fraud detection system to identify.

---

## Features

### OTP Lifecycle Management

- Generates a new 6-digit OTP for every transaction.
- OTPs automatically expire after a fixed period.
- An OTP can only be consumed once.
- OTP verification is synchronized to safely handle concurrent verification
  attempts.
- OTP expiry is handled in the background using `ScheduledExecutorService`.

### Thread-Safe Transactions

- Account balance updates are synchronized.
- Locks are acquired in a consistent order to reduce concurrency problems.
- Multiple threads can attempt transactions concurrently.
- The project includes a stress test where five threads attempt to withdraw
  money from the same account simultaneously.
- The test checks that the account does not become negative or spend the same
  balance twice.

### Concurrent Fraud Detection

- Fraud detection runs separately from the main transaction process.
- Completed transactions are placed into a `BlockingQueue`.
- A dedicated background thread consumes transactions from the queue.
- Transactions are checked using the implemented `FraudRule` classes.
- Fraud detection does not block the main transfer process.

### Encrypted Transaction History

- Transaction history can be exported in encrypted form.
- AES encryption is used to protect exported transaction history.
- Encryption uses Java's built-in `javax.crypto` package.
- No separate encryption library is required for AES encryption.

### Database and Audit Logging

- Transaction information is stored in a SQLite database using JDBC.
- Fraud alerts are also stored in the database.
- A human-readable activity log records system activity.
- The database is file-based, so no separate database server is required.

---

## Technologies / Tools Used

| Technology / Tool | Purpose |
|---|---|
| **Java 17+** | Main programming language |
| **JDBC** | Database connectivity |
| **SQLite** | Persistent storage for transactions and fraud alerts |
| **java.util.concurrent** | Multithreading and concurrency |
| `ScheduledExecutorService` | Background OTP expiry |
| `BlockingQueue` | Communication between transactions and fraud detection |
| `ConcurrentHashMap` | Thread-safe data management |
| `CountDownLatch` | Coordination in concurrency testing |
| **javax.crypto** | AES encryption |
| **JDK standard libraries** | Core functionality |

The project does not use a Java framework.

---

## Project Structure

```text
SecureBankOTP/
├── diagrams/                      # Project design diagrams
├── lib/
│   └── sqlite-jdbc.jar            # SQLite JDBC driver
├── src/com/securebank/
│   ├── bank/                      # Bank simulation logic
│   ├── exceptions/                # Custom exceptions
│   ├── fraud/                     # Fraud detection rules and engine
│   ├── model/                     # User, Account, Transaction and OTP models
│   ├── otp/                       # OTP generation, verification and expiry
│   ├── persistence/               # SQLite database and audit classes
│   ├── security/                  # AES encryption utilities
│   └── util/                      # Logging utilities
├── Main.java                      # Program entry point / demonstration
├── README.md                      # Project documentation
└── statement.md                   # Project statement
