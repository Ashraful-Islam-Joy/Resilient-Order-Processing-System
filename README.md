# 🛡️ Resilient Order Processing System

A high-throughput, fault-tolerant Distributed Order Processing Microservice built with **Java 21** and **Spring Boot 4.1.1**.

This system solves real-world distributed microservice challenges:
1. **Preventing Double-Spending & Duplicate Requests** (Idempotency via Redis)
2. **Preventing Race Conditions & Inventory Overselling** (Pessimistic Database Locking via PostgreSQL)
3. **Preventing Cascading Failures** (Circuit Breaker & Fallbacks via Resilience4j)

---

## 🏛️ System Architecture & Engineering Decisions

### 1. Request Deduplication (Redis Idempotency)
When network latency causes clients to retry orders, duplicate submissions happen. The service verifies the `Idempotency-Key` header in Redis prior to processing. If present, the cached result is returned instantly without database or downstream interaction.

### 2. High-Concurrency Inventory Control (PostgreSQL Pessimistic Locking)
To eliminate stock overselling during flash sales, the system uses PostgreSQL `SELECT FOR UPDATE` (`LockModeType.PESSIMISTIC_WRITE`). Concurrent transactions requesting the same inventory row are safely queued until the lock releases.

### 3. Graceful Service Degradation (Resilience4j Circuit Breaker)
Downstream payment gateways are prone to failure. Resilience4j tracks operational health—if calls fail or time out, the circuit opens and redirects to a **Fallback Mechanism**, marking status gracefully as `PAYMENT_FAILED` without server exceptions.

---

## 🛠️ Tech Stack

* **Language:** Java 21
* **Framework:** Spring Boot 4.1.1 (Spring Data JPA, Spring Data Redis, Spring Web)
* **Database:** PostgreSQL (Pessimistic Locking)
* **Caching & Idempotency:** Redis
* **Fault Tolerance:** Resilience4j (Circuit Breaker & Fallbacks)
* **Build & Tools:** Lombok, Maven

---

## 📁 Repository Structure

```text
src/main/java/com/embarkx/orderservice/
├── controller/       # REST API Endpoints (OrderController)
├── dto/              # Request & Response Data Objects
├── model/            # JPA Entities (Order, Product, OrderStatus)
├── repository/       # Data Access with Pessimistic Lock queries
└── service/          # Core Business Logic & Payment Integration



🚀 Local Setup Guide

*Prerequisites
*Java 21 JDK
*PostgreSQL (localhost:5432)
*Redis (localhost:6379)


🧪 Quick API Verification & Resiliency Testing

Test 1: Standard Order Creation

A unique Idempotency-Key is sent to verify the standard workflow. The system queries the database, locks the inventory, triggers the payment, and responds with either COMPLETED or PAYMENT_FAILED.

curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: order-key-101" \
  -d '{"productId": 1, "quantity": 1}'


  Test 2: Idempotency (Duplicate Request Prevention)
If the customer or app sends the exact same request twice due to a network delay, the same order-key-101 is used.

curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: order-key-101" \
  -d '{"productId": 1, "quantity": 1}'

*Explanation: Even if the same request is sent twice due to network delay, the system checks the Redis key and stops secondary database queries.

*Expected Output: Instead of executing the database query, the system will instantly handle the duplicate request using the response from the Redis cache.


Test 3: Circuit Breaker & Fallback System Test
A dynamic fallback test is conducted to check whether the system will collapse if the downstream Payment Gateway fails.

curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: order-key-102" \
  -d '{"productId": 1, "quantity": 1}'

*Explanation: If the payment gateway becomes unresponsive or fails, the Resilience4j Circuit Breaker intercepts the request and routes it to the fallback method.

*Expected Output: Instead of crashing, the system will gracefully process the order status as PAYMENT_FAILED.