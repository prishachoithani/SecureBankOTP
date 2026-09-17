# Problem Statement

## The Problem

Online banking fraud via OTP (One-Time Password) manipulation is one of
the most common forms of financial fraud in India today. A typical
attack unfolds like this: a scammer contacts a victim posing as a bank
representative, KYC agent, or customer support executive, and convinces
them to share the OTP sent to their phone — usually under the pretext
of "verifying" their account or resolving a fake issue. The moment the
victim reads out the OTP, the scammer uses it to authorize a
transaction from a device the victim never approved, often draining the
account in a single large transfer or a rapid sequence of smaller ones
before the victim realizes what happened.

Traditional OTP systems check only one thing: *is the code correct?*
They have no concept of *is this transaction behaving like fraud*, even
when the technically-valid OTP is being used from an unrecognized
device, for an unusually large amount, or as part of a rapid burst of
transfers — all classic markers of an in-progress OTP scam.

## Scope of the Project

This project builds a simulated banking transaction system that closes
that gap. It does not simulate the attack itself — it simulates the
**defensive system** a real bank would need: OTP-gated transfers, with
a concurrent fraud-detection layer that screens every transaction for
the behavioral patterns associated with OTP fraud, independent of
whether the OTP itself was correct.

In scope:
- Account and user management
- OTP generation, expiry, and single-use enforcement
- Thread-safe money transfers (no double-spending under concurrency)
- Rule-based fraud detection (high amount, rapid transactions,
  unrecognized device) running on a background thread
- Encrypted, persistent audit trail (JDBC + AES)

Out of scope (by design, for an academic simulation):
- Real SMS/push OTP delivery (stubbed with a demo-only method)
- Real user authentication/login system
- A GUI or web front-end (console-driven demo)

## Target Users

- **Primary:** Bank account holders who could fall victim to OTP-based
  social-engineering fraud.
- **Secondary:** Bank fraud-operations teams who would monitor the
  `fraud_alerts` table / activity log to intervene on flagged
  transactions.

## High-Level Features

1. **OTP-gated transfers** — no transaction completes without a valid,
   unexpired, single-use OTP.
2. **Asynchronous fraud screening** — every completed transaction is
   independently screened by a background thread against multiple
   fraud heuristics, without slowing down the transfer itself.
3. **Concurrency-safe balances** — synchronized withdrawal/deposit logic
   with consistent lock ordering prevents race conditions when multiple
   transfers touch the same account simultaneously.
4. **Encrypted, auditable history** — every transaction and fraud alert
   is persisted to a database (JDBC) and account histories can be
   exported as AES-encrypted files.
