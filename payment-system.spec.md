# **Technical Specification: High-Performance and Secure Payment System**

**Project:** High-Performance Payment Platform (Core Banking & Wallet Engine)  
**Date:** May 30, 2026  
**Status:** Detailed Architecture Design (Production-Ready Specification)  
**Version:** 2.0.0

## **1\. System Overview**

The system is designed to meet the strictest standards for security, absolute data integrity (ACID) at the account balance level, and high concurrency capabilities during flash sales or sudden traffic spikes, along with automatic reconciliation capabilities. The architecture adopts a Microservices model, asynchronous communication via the Kafka Event Bus message queue, and state synchronization via Transactional Outbox and Inbox patterns.

## **2\. Detailed Technical Specifications by Module**

### **2.1. Authentication & Security Module (Auth & Security)**

Ensures absolute safety for user account information and comprehensive protection of service endpoints against vulnerability exploitation.

* **Spring Security Configuration:** Establishes a strict Security Filter Chain to control all incoming requests. Disables CSRF as the system uses Stateless APIs, applies a strict CORS (Cross-Origin Resource Sharing) policy, and integrates standard security headers (X-Content-Type-Options, X-Frame-Options, Content-Security-Policy).  
* **Argon2id / BCrypt Password Hashing:** All user passwords are hashed before storage using strong cryptographic algorithms. The system prioritizes Argon2id with optimal parameters (Iterations \= 3, Memory \= 64MB, Parallelism \= 4\) to resist hardware-accelerated Brute-force or Rainbow Table attacks.  
* **JWT Access & Refresh Token Management:** Implements a Dual-Token Pattern. The Access Token has a short lifespan (15 minutes) and is signed with the RS256 asymmetric algorithm. The Refresh Token has a long lifespan (7 days), stored encrypted in Redis/Database, paired with Token Rotation (revoking all old tokens if a Refresh Token is used abnormally).  
* **Rate Limiting Mechanism:** Prevents DoS/DDoS and brute-force attacks at the API Gateway using Bucket4j combined with Redis. Applies the Token Bucket algorithm based on IP address and User ID:

| API Endpoint Type | Maximum Limit (Requests/Minute) | Handling Mechanism   |
| :---- | :---- | :---- |
| Register / Login / Change Password | 5 requests / IP | HTTP Status 429 Too Many Requests, temporary IP block for 15 minutes if violated continuously. |
| Initiate Transaction / Deposit / Transfer | 10 requests / User | HTTP Status 429, refuses processing and queues the ID for risk monitoring. |
| Query History / Balance | 60 requests / User | HTTP Status 429, prioritizes returning data from cache if available. |

### **2.2. Database Architecture & Balance Data Consistency**

Managing the Wallet Balance requires absolute precision, with no tolerance for data discrepancies, even under high distributed load.

* **Database Architecture for Balance:** Relational database design (PostgreSQL) based on the Ledger principle. The available balance is maintained in the wallet table, strictly parallel with the wallet\_transaction table recording detailed balance changes. All financial actions must be recorded as double-entry accounting or immutable logs for the audit trail.  
* **Pessimistic Locking in Spring Data JPA:** To prevent Race Conditions when multiple concurrent requests attempt to deduct money from the same account, the system uses a Pessimistic Write Lock (SELECT ... FOR UPDATE) via Spring Data JPA's @Lock(LockModeType.PESSIMISTIC\_WRITE) annotation. The processing flow ensures absolute ACID properties.

### **2.3. High Concurrency Processing with Redis (Distributed Lock)**

During sudden traffic spikes (e.g., Flash Sales), directly locking the Database layer can exhaust the DB Connection Pool, causing system bottlenecks. The system establishes a distributed locking layer using Redis ahead of the DB layer.

* **Redisson (Redis Distributed Lock):** Uses Redisson to create distributed locks on the Redis Cluster. For each transaction request affecting the Wallet, the system acquires a distributed lock with a unique key identified by the wallet (e.g., lock:wallet:{wallet\_id}) with an automatic release mechanism (leaseTime) to prevent deadlocks if the processing node crashes.  
* **Atomic Lua Script:** To optimize fast processing, the system maintains a temporary wallet balance in Redis RAM. The balance check and deduction flow are fully encapsulated in a Lua Script to ensure absolute atomicity (running single-threaded on Redis), completely eliminating intermediate network I/O overhead.

\-- Atomic Lua Script to check conditions and deduct balance quickly on Redis  
local wallet\_key \= KEYS\[1\]  
local tx\_amount \= tonumber(ARGV\[1\])  
local current\_balance \= tonumber(redis.call('get', wallet\_key) or "0")

if current\_balance \>= tx\_amount then  
    local new\_balance \= current\_balance \- tx\_amount  
    redis.call('set', wallet\_key, tostring(new\_balance))  
    return new\_balance \-- Returns the new balance (Success)  
else  
    return \-1 \-- Returns an error code indicating insufficient balance  
end

### **2.4. Absolute Idempotency Mechanism (Deduplication)**

Ensures a financial transaction is processed only once, eliminating the risk of users clicking the pay button multiple times due to network lag or the client automatically retrying the packet.

* **Idempotency Layer Architecture:** Builds an intermediary filter blocking all financial transaction initiation requests. Every request must attach a header containing the Idempotency-Key (a unique UUID v4 generated by the Client).  
* **Deduplication Mechanism:** The system checks the existence of the Idempotency-Key in Redis (TTL configured for 24 hours). If it exists and has a COMPLETED status, the system directly reads the old Response Body and returns it immediately without re-running any underlying deduction logic.

### **2.5. Gateway Integration & Webhook Security (Stripe)**

Connects securely to the Stripe international payment gateway and establishes a strict defense system for asynchronous response data streams.

* **Stripe API Integration:** Uses the official Stripe SDK for Java to execute advanced payment flows. The backend initializes a PaymentIntent and returns a Client Secret for the front-end to directly submit the card to Stripe's servers, ensuring the internal system does not store raw card data to strictly comply with PCI-DSS.  
* **Secure Webhook Handling & Replay Attack Prevention:** Uses Stripe Signature Verification to verify the signature of incoming webhooks. Extracts the timestamp from the Stripe-Signature header and compares it with the server's time to prevent Replay Attacks. Additionally, the IDs of processed Webhook events are stored to prevent duplicate processing.

### **2.6. Event-Driven Data Consistency: Transactional Outbox & Inbox Patterns**

Ensures Eventual Consistency and Exactly-Once Processing between the local database and the Kafka Event Bus without data loss or duplicate processing.

* **Transactional Outbox Pattern (At-Least-Once Delivery):** When a financial transaction succeeds, instead of directly publishing a message to Kafka (which risks data inconsistency if Kafka is down), the event data is inserted into an outbox\_table within the same Local Database Transaction as the wallet balance update. A background worker (CDC or Scheduler) scans this table and safely pushes messages to Kafka.  
* **Inbox Pattern (Idempotent Consumer):** On the receiving end, to handle potential duplicate messages from Kafka, the consumer checks the incoming message ID against an inbox\_table in its own database. The processing of the message and the insertion of the ID into the inbox\_table happen within a single transaction. If the ID already exists, the message is ignored, achieving **Exactly-Once Processing**.

### **2.7. Real-time Fraud Detection System**

Monitors and analyzes transaction behavior streams in real-time to prevent money laundering, account hacking, or payment fraud.

* **Spring Kafka Listener:** Builds an independent Microservice dedicated to Fraud Detection, continuously consuming financial events from the transaction-events Kafka Topic.  
* **Velocity Check with Sliding Window:** Applies the Sliding Window technique via Kafka Streams or short-term data storage in Redis Sorted Sets to accurately measure the transaction frequency and behavior of an account identifier over a continuous time frame.

| Risk Rule | Threshold | Immediate Action   |
| :---- | :---- | :---- |
| Spamming / High Frequency | More than 5 transactions within 10 seconds | Automatically freezes the wallet account, changes the current transaction status to FRAUD\_REJECTED. |
| Sudden Expenditure Spike | Total expenditure in 5 minutes exceeds 500% of the daily average | Pauses the transaction, sends an activation code for Multi-Factor Authentication (MFA). |
| Velocity Location Risk | 2 transactions over 100km apart within 10 minutes | Activates card lock, sends an emergency security alert to the registered mobile device. |

### **2.8. State Management & Transaction Lifecycle**

Strictly controls the entire lifecycle of a financial transaction, preventing ambiguous data or unauthorized status changes via Spring State Machine (PENDING \-\> PROCESSING \-\> SUCCESS/FAILED/FRAUD\_REJECTED).

### **2.9. High-Load Notification System & Backpressure**

Sends balance change notifications and transaction receipts to customers quickly without affecting the latency of the core payment flow.

* **Asynchronous Load Consumption:** The independent Notification Service consumes events from the notification-events Kafka Topic.  
* **ThreadPoolTaskExecutor & Backpressure:** Configures Thread pools with bounded queue capacities and the CallerRunsPolicy rejection handler to create Backpressure and prevent Out Of Memory (OOM) errors during traffic surges.

### **2.10. Automated Reconciliation System**

Acts as the final audit checkpoint, automatically detecting and repairing financial data discrepancies between the internal system and the payment partner (Stripe).

* **Two-Way Matching Process:** Uses Spring Batch to automatically schedule a daily job that downloads transaction reports from Stripe. It matches internal success records against partner records (Internal-to-Partner) and vice versa (Partner-to-Internal) to detect any missing or mismatched transactions, ensuring eventual consistency.

## **3\. System Performance KPI Targets**

* **Latency:** Under 200ms for 99% (p99) of APIs related to balance checks and core transaction initiation under normal conditions.  
* **Throughput:** Minimum 5,000 successful transactions per second (TPS) across the system without balance discrepancies.  
* **High Availability:** Uptime of 99.99%, eliminating all Single Points of Failure with architectural redundancy.