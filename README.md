# SecureBank OTP — Fraud-Resilient Transaction Simulator

A Java simulation of a bank's OTP-verified money transfer system with a
concurrent, rule-based fraud detection engine layered on top — built to
model (and defend against) the real-world problem of **OTP-based
account-draining fraud**, where a scammer tricks a victim into reading
out a one-time password and uses it to authorize an unauthorized transfer.

> Built for CSE2006 (Programming in Java), VIT Bhopal.

---

## Overview

Every transfer in this system must clear a two-step gate before funds
move:

### 1. OTP Verification

A 6-digit, time-bound, single-use code is issued for every transaction.

The OTP:

- Is generated for each transaction.
- Is valid only for a limited amount of time.
- Can only be used once.
- Is automatically expired by a background thread.
- Uses synchronized consumption so that the same OTP cannot be successfully
  used multiple times, even under concurrent verification attempts.

### 2. Fraud Screening

After a transaction is completed, it is processed by a separate background
fraud-detection thread.

The fraud engine checks completed transactions against implemented rules,
including:

- An unusually high transfer amount compared to the account's previous
  transaction history.
- Rapid consecutive transfers happening within a short period.

If a transaction matches a fraud rule, an alert is generated and recorded
in the audit logs.

The project deliberately models the OTP-fraud attack pattern by performing
a small legitimate transfer followed by an unusually large transfer from
the same account. This provides a realistic suspicious transaction for the
fraud detection engine to identify.

---

## Features

### OTP Lifecycle Management

- Generates a new OTP for each transaction.
- OTPs expire automatically after a fixed period.
- An OTP cannot be used more than once.
- OTP verification is synchronized to safely handle concurrent verification
  attempts.
- OTP expiry is handled in the background using `ScheduledExecutorService`.

### Thread-Safe Transfers

- Account balance updates are synchronized.
- Locks are acquired in a consistent order to avoid concurrency problems.
- Concurrent transfers are handled safely.
- The project includes a stress test where five threads attempt to withdraw
  money from the same account simultaneously.
- The test checks that the account cannot be overdrawn or spend the same
  money twice.

### Concurrent Fraud Detection

- Fraud detection runs separately from the transaction itself.
- Completed transactions are placed into a `BlockingQueue`.
- A dedicated background thread consumes transactions from the queue.
- Transactions are checked using the implemented `FraudRule` classes.
- The fraud detection process does not block the main transfer path.

### Encrypted Transaction History

- Transaction history can be exported in encrypted form.
- AES encryption is used to protect exported account history.
- Encryption uses Java's built-in `javax.crypto` package.
- No external encryption library is required.

### Persistent Audit Trail

- Transaction information is stored in a SQLite database using JDBC.
- Fraud alerts are also stored in the SQLite database.
- A human-readable activity log records system activity.
- The database is file-based, so no separate database server is required.

---

## Technologies / Tools Used

- **Java 17+** — core programming language.
- **JDBC** — database connectivity.
- **SQLite** — file-based database for transaction and fraud-alert records.
- **`java.util.concurrent`** — multithreading and concurrency.
  - `ScheduledExecutorService` — scheduled OTP expiry.
  - `BlockingQueue` — communication between transactions and fraud detection.
  - `ConcurrentHashMap` — thread-safe data management.
  - `CountDownLatch` — coordination during concurrency testing.
- **`javax.crypto`** — AES encryption using the standard Java JDK.
- **SQLite JDBC driver** — allows Java to communicate with the SQLite database.

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
│   ├── fraud/                     # Fraud rules and detection engine
│   ├── model/                     # User, Account, Transaction and OTP
│   ├── otp/                       # OTP generation and expiry
│   ├── persistence/               # SQLite database and audit classes
│   ├── security/                  # AES encryption
│   └── util/                      # Logging utilities
├── Main.java                      # Demo driver / entry point
├── README.md                      # Project documentation
└── statement.md                   # Project statement
```

---

# Installation & Running

## Requirements

The project requires:

- **JDK 17 or later**
- **SQLite JDBC driver**

The SQLite JDBC driver is already included in the repository inside:

```text
lib/sqlite-jdbc.jar
```

No separate SQLite database server is required.

---

## Step 1: SQLite JDBC Driver

The required SQLite JDBC driver is already provided in the project's
`lib/` directory:

```text
lib/sqlite-jdbc.jar
```

Therefore, there is no need to download or install a separate database
server.

---

## Step 2: Compile the Project

Open a terminal in the project root directory.

### Linux / macOS

```bash
javac -cp "lib/*" -d out $(find src -name "*.java") Main.java
```

### Windows

The source files can be compiled from the project root using the SQLite
driver in the `lib` directory.

The important point is that Windows uses `;` instead of `:` as the
classpath separator.

---

## Step 3: Run the Project

### Linux / macOS

```bash
java -cp "out:lib/*" Main
```

### Windows

```bash
java -cp "out;lib/*" Main
```

---

## What Should Happen When You Run It?

The program demonstrates several different situations:

- Creates and sets up an account.
- Generates and verifies an OTP.
- Performs a normal/legitimate transfer.
- Performs a suspiciously large transfer.
- Runs the fraud detection engine in the background.
- Runs the concurrency stress test using five threads.
- Creates an encrypted transaction history file.

---

## Screenshot of Program Output

**Insert a screenshot of the successful console output here.**

The screenshot should preferably show the major stages of the program,
including the normal transfer, suspicious transaction/fraud detection,
concurrency test and encrypted export.

```text
[ INSERT PROGRAM OUTPUT SCREENSHOT HERE ]
```

---

# Generated Output Files

After the program finishes, the following files can be checked.

### Activity Log

```text
data/bank_activity.log
```

This contains the full timestamped activity log.

### SQLite Database

```text
data/securebank.db
```

The SQLite database contains:

```text
transactions
fraud_alerts
```

It can be opened with any SQLite database browser or the SQLite command-line
tool.

For example:

```bash
sqlite3 data/securebank.db
```

### Encrypted Transaction History

```text
data/ACC1001_history.enc
```

This file contains the AES-encrypted transaction history.

---

## Screenshot of Generated Files

**Optional: Insert a screenshot showing the generated files here.**

```text
[ INSERT SCREENSHOT OF data/ FOLDER HERE ]
```

---

# Instructions for Testing

The included `Main.java` doubles as an integration test, exercising the
main parts of the project.

| Scenario | What it proves |
|---|---|
| **Legitimate transfer** | Correct OTP → funds move, transaction is logged, and no fraud flag is generated |
| **Suspicious transfer (unusually large amount)** | The fraud engine correctly flags a valid-but-suspicious transaction asynchronously, without blocking the transfer |
| **Concurrency stress test** | Five threads withdraw simultaneously from one account; the final balance demonstrates that no double-spend or negative balance occurs |
| **Encrypted export** | AES round-trip: data is encrypted to disk, decrypted back, and the content matches the original |

---

## Test 1: Legitimate Transfer

The program first performs a normal transaction using the correct OTP.

Expected flow:

```text
OTP generated
     ↓
OTP verified
     ↓
Transfer completed
     ↓
Funds moved
     ↓
Transaction logged
```

The transaction should complete successfully without triggering the
implemented suspicious-transaction rules.

---

## Test 2: Suspicious Transfer

The program then performs an unusually large transfer from the same account.

The transaction is completed and then passed to the background fraud
detection engine.

Expected flow:

```text
Transaction completed
        ↓
Transaction placed in BlockingQueue
        ↓
Background fraud detection thread
        ↓
Fraud rules applied
        ↓
Fraud alert generated if a rule matches
```

The fraud detection result can be checked in:

```text
data/bank_activity.log
```

and in the:

```text
fraud_alerts
```

table inside:

```text
data/securebank.db
```

---

## Test 3: Concurrency Stress Test

The project includes a concurrency test in which five threads attempt to
withdraw money from the same account at the same time.

This test demonstrates that synchronized account operations prevent:

- Negative account balance.
- Double-spending.
- Incorrect balance updates caused by concurrent access.

The final account balance shown by the program can be used to verify the
result.

---

## Test 4: Encrypted Export

The program exports the account's transaction history using AES encryption.

The test performs an encryption/decryption round trip:

```text
Original transaction history
          ↓
       AES encrypt
          ↓
Encrypted file on disk
          ↓
       AES decrypt
          ↓
Compare with original
```

The encrypted transaction history is stored as:

```text
data/ACC1001_history.enc
```

The test verifies that the decrypted content matches the original content.

---

## Running Custom Test Cases

To add your own test cases, call:

```java
BankSimulator.initiateTransfer(...)
```

and:

```java
verifyAndComplete(...)
```

with different transaction amounts and transaction details.

The resulting fraud detection activity can be inspected in:

```text
data/bank_activity.log
```

and the database:

```text
data/securebank.db
```

---

## Screenshot of Testing / Results

**Optional: Insert a screenshot showing the testing results here.**

```text
[ INSERT TEST RESULTS SCREENSHOT HERE ]
```

---

# OTP Demonstration Note

There is a method called:

```java
peekOtpForDemo()
```

in `BankSimulator`.

This exists only because this is a simulation and there is no actual SMS
or notification service connected to the project.

It allows the demo program to get the OTP and continue the transaction flow.

In a real banking application, the OTP would be sent to the user's
registered device through a secure delivery service and would not be exposed
through a method like this.

---

# Database

SQLite is used because it keeps the project simple to run.

There is no separate database server that needs to be installed or started.
The database is stored as a file inside the `data` directory.

The database location is:

```text
data/securebank.db
```

The database contains transaction and fraud-alert records.

It can be opened using any SQLite database browser or the SQLite command-line
tool.

Example:

```bash
sqlite3 data/securebank.db
```

---

# Purpose of the Project

This project was mainly built to understand how different Java concepts work
together in a practical application.

In particular, it demonstrates:

- Multithreading.
- Synchronization and concurrency.
- OTP handling.
- Exception handling.
- JDBC and SQLite.
- Background processing with queues.
- Basic rule-based fraud detection.
- AES encryption.
- Logging and audit records.

The project is a simulation for learning purposes and is **not intended to
be used as an actual banking system**.

---

# Project Demonstration Screenshot

**Optional: Add a final screenshot showing the complete successful run.**

```text
[ INSERT FINAL PROJECT SCREENSHOT HERE ]
```
