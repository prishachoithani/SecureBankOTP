# SecureBank OTP — Fraud-Resilient Transaction Simulator

A Java simulation of a bank's OTP-verified money transfer system with a
concurrent, rule-based fraud detection engine layered on top — built to
model (and defend against) the real-world problem of **OTP-based
account-draining fraud**, where a scammer tricks a victim into reading
out a one-time password and uses it to authorize an unauthorized transfer.




# Overview
 
Every transfer passes a mandatory two-stage gate before it is finalised.
 
### 1. OTP Verification
 
Each transaction is bound to a unique six-digit one-time password that is time-bound and single-use. A background thread expires stale codes, and consumption is synchronized so the same OTP cannot succeed twice under concurrent verification attempts.
 
### 2. Fraud Screening
 
Completed transactions are reviewed asynchronously by a background fraud-detection engine, keeping analysis off the transfer path. Current rules flag:
 
- **Anomalous amount** — the transfer deviates sharply from the account's transaction history.
- **Rapid consecutive transfers** — multiple transfers from one account within a short window.
A rule match raises an alert and records it in the audit log.
 
### Demonstration Scenario
 
The project reproduces a known OTP-fraud pattern — a small legitimate transfer followed by an unusually large one from the same account — providing a realistic suspicious transaction for the engine to detect.


# Features

* **OTP Lifecycle Management:** Generates unique OTPs for each transaction, with automatic expiry and single-use validation. Verification is synchronized for concurrent requests and expiry is managed using `ScheduledExecutorService`.

* **Thread-Safe Transfers:** Ensures synchronized balance updates and consistent lock ordering to prevent concurrency issues, double spending, and account overdrafts.

* **Concurrent Fraud Detection:** Processes completed transactions asynchronously through a `BlockingQueue`, allowing fraud detection to run independently without blocking transactions.

* **Encrypted Transaction History:** Exports transaction history securely using AES encryption through Java’s built-in `javax.crypto` package.

* **Persistent Audit Trail:** Stores transactions and fraud alerts in a file-based SQLite database using JDBC, along with a human-readable activity log.




# Technologies / Tools Used

- **Java 17+** — core programming language.
- **JDBC** — database connectivity.
- **SQLite** — file-based database for transaction and fraud-alert records.
- **java.util.concurrent** — multithreading and concurrency.
  - ScheduledExecutorService — scheduled OTP expiry.
  - BlockingQueue — communication between transactions and fraud detection.
  - ConcurrentHashMap — thread-safe data management.
  - CountDownLatch — coordination during concurrency testing.
- **javax.crypto** — AES encryption using the standard Java JDK.
- **SQLite JDBC driver** — allows Java to communicate with the SQLite database.

The project does not use a Java framework.



# Project Structure

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



## Step 1: SQLite JDBC Driver

The required SQLite JDBC driver is already provided in the project's
`lib/` directory:

```text
lib/sqlite-jdbc.jar
```

Therefore, there is no need to download or install a separate database
server.



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



## Step 3: Run the Project

### Linux / macOS

```bash
java -cp "out:lib/*" Main
```

### Windows

```bash
java -cp "out;lib/*" Main
```



# Screenshot of Program Output

<img width="2512" height="1418" alt="Screenshot 2026-09-17 104359" src="https://github.com/user-attachments/assets/f000b31f-af0d-47d0-886a-a65476d98709" />
<img width="2038" height="592" alt="Screenshot 2026-09-17 104421" src="https://github.com/user-attachments/assets/494d9139-5739-4d43-97a3-45e22a8cac8e" />




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




# Instructions for Testing

The included `Main.java` doubles as an integration test, exercising the
main parts of the project.

| Scenario | What it proves |
|---|---|
| **Legitimate transfer** | Correct OTP → funds move, transaction is logged, and no fraud flag is generated |
| **Suspicious transfer (unusually large amount)** | The fraud engine correctly flags a valid-but-suspicious transaction asynchronously, without blocking the transfer |
| **Concurrency stress test** | Five threads withdraw simultaneously from one account; the final balance demonstrates that no double-spend or negative balance occurs |
| **Encrypted export** | AES round-trip: data is encrypted to disk, decrypted back, and the content matches the original |



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


