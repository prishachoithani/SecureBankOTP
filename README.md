# SecureBank OTP — Fraud-Resilient Transaction Simulator

A Java simulation of a bank's OTP-verified money transfer system with a
concurrent, rule-based fraud detection engine layered on top — built to
model (and defend against) the real-world problem of **OTP-based
account-draining fraud**, where a scammer tricks a victim into reading
out a one-time password and uses it to authorize an unauthorized transfer.

> Built for CSE2006 (Programming in Java), VIT Bhopal.

## Overview

Every transfer in this system must clear a two-step gate before funds
move:

1. **OTP verification** — a 6-digit, time-bound, single-use code, issued
   per transaction and auto-expired by a background thread.
2. **Fraud screening** — a background thread that inspects every
   completed transaction against a set of pluggable rules (unusually
   high amount relative to the account's history, rapid consecutive
   transfers) and raises an audit-logged alert if any rule fires.

The project deliberately models the OTP-fraud attack pattern — a small
legitimate transfer followed by an unusually large one on the same
account — so the fraud engine has something real to catch.

## Features

- **OTP lifecycle management** — codes are generated, time-limited, and
  auto-expired using `ScheduledExecutorService`; consumption is
  synchronized so a code can never be used twice, even under concurrent
  verification attempts.
- **Thread-safe transfers** — account balance updates are synchronized
  with consistent lock ordering, so concurrent transfers can never
  double-spend or push a balance negative (proven by an included stress
  test that fires 5 simultaneous withdrawal threads at one account).
- **Concurrent fraud detection** — a dedicated background thread
  consumes completed transactions from a `BlockingQueue` and applies a
  strategy-pattern set of `FraudRule` implementations without blocking
  the transfer path.
- **Encrypted transaction history** — AES encryption (JDK's built-in
  `javax.crypto`, no external dependency) protects exported account
  history files at rest.
- **Persistent audit trail** — all transactions and fraud alerts are
  written to a SQLite database via JDBC, plus a human-readable activity
  log file.

## Technologies / Tools Used

- Java 17+ (core language, no framework)
- JDBC with [SQLite](https://github.com/xerial/sqlite-jdbc) (file-based,
  zero-config database)
- `java.util.concurrent` (`ScheduledExecutorService`, `BlockingQueue`,
  `ConcurrentHashMap`, `CountDownLatch`)
- `javax.crypto` (AES encryption, part of the standard JDK)

## Project Structure

```
src/com/securebank/
├── Main.java                      # demo driver / entry point
├── model/                         # User, Account, Transaction, OTP
├── exceptions/                    # custom checked exceptions
├── otp/                           # OTPManager (issues OTPs, schedules their expiry)
├── fraud/                         # FraudRule + 2 implementations, FraudDetectionEngine (threaded)
├── security/                      # EncryptionUtil (AES)
├── persistence/                   # DatabaseManager, AuditDAO (JDBC)
├── bank/                          # BankSimulator (orchestrator)
└── util/                          # BankLogger
```

## Steps to Install & Run

**Requirements:** JDK 17 or later.

1. Download the SQLite JDBC driver jar and place it in a `lib/` folder
   in the project root:
   [sqlite-jdbc-3.36.0.3.jar](https://github.com/xerial/sqlite-jdbc/releases/download/3.36.0.3/sqlite-jdbc-3.36.0.3.jar)
   *(any recent sqlite-jdbc release works; this version was used for
   testing because it has no extra runtime dependencies.)*

2. Compile:
   ```bash
   javac -cp "lib/sqlite-jdbc.jar" -d out $(find src -name "*.java")
   ```

3. Run:
   ```bash
   java -cp "out:lib/sqlite-jdbc.jar" com.securebank.Main
   ```
   *(On Windows, use `;` instead of `:` in the classpath.)*

4. Check the output:
   - Console shows the full run: account setup, a legitimate transfer,
     a suspicious transfer that gets fraud-flagged, a 5-thread
     concurrency stress test, and an encrypted export.
   - `data/bank_activity.log` — full timestamped activity log.
   - `data/securebank.db` — SQLite database with `transactions` and
     `fraud_alerts` tables (open with any SQLite browser, or `sqlite3
     data/securebank.db`).
   - `data/ACC1001_history.enc` — AES-encrypted transaction history.

## Instructions for Testing

The included `Main.java` doubles as an integration test, exercising:

| Scenario | What it proves |
|---|---|
| Legitimate transfer | Correct OTP → funds move, transaction logged, no fraud flag |
| Suspicious transfer (unusually large amount) | Fraud engine correctly flags a valid-but-suspicious transaction asynchronously, without blocking it |
| Concurrency stress test | 5 threads withdraw simultaneously from the same account; final balance proves no double-spend or negative balance occurred |
| Encrypted export | AES round-trip: encrypt to disk, decrypt back, content matches |

To add your own test cases, call `BankSimulator.initiateTransfer(...)`
and `verifyAndComplete(...)` with different amounts and inspect
`data/bank_activity.log` for the fraud engine's verdict.

## Notes

- `peekOtpForDemo()` on `BankSimulator` is a **demo-only** stand-in for
  an SMS/push notification gateway — a real deployment would never
  expose the OTP to a caller; it would only leave the server via the
  delivery channel.
- The database is file-based (SQLite) specifically so this project runs
  standalone with zero setup — no server to install or start.
