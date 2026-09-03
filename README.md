# Spring Boot Banking Application

A secure, production-oriented REST API for core banking operations, built with **Java 21, Spring Boot 4, Spring Security, Spring Data JPA, JWT, MySQL, and OpenAPI**.

The project demonstrates backend engineering practices beyond basic CRUD, including stateless authentication, role- and resource-level authorization, account lifecycle rules, transactional money transfers, pessimistic locking, centralized error handling, automated testing, and interactive API documentation.

---

## Overview

The Spring Boot Banking Application provides a RESTful backend for managing users, bank accounts, and financial transactions.

The system supports:

- User registration and authentication
- JWT-based stateless security
- `USER` and `ADMIN` authorization
- Account ownership protection
- Savings and current accounts
- Account lifecycle management
- Deposits and withdrawals
- Account-to-account transfers
- Transaction history with pagination and filtering
- OpenAPI/Swagger documentation
- Unit, integration, and security testing

The application follows a layered architecture to keep HTTP handling, business logic, persistence, security, and API contracts clearly separated.

---

## Tech Stack

| Category | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.1.1 |
| Web | Spring Web MVC |
| Security | Spring Security |
| Authentication | JWT using JJWT 0.12.6 |
| Persistence | Spring Data JPA |
| ORM | Hibernate |
| Production Database | MySQL |
| Test Database | H2 |
| Validation | Jakarta Bean Validation |
| API Documentation | OpenAPI 3 / Swagger UI |
| OpenAPI Integration | springdoc-openapi |
| Unit Testing | JUnit 5, Mockito |
| Integration Testing | Spring Boot Test, MockMvc |
| Build Tool | Maven |

---

## Architecture

The application follows a traditional layered backend architecture:

```text
                    ┌─────────────────────┐
                    │       Client        │
                    └──────────┬──────────┘
                               │ HTTP / JSON
                               ▼
                    ┌─────────────────────┐
                    │     Controllers     │
                    └──────────┬──────────┘
                               │
                               ▼
                    ┌─────────────────────┐
                    │      Services       │
                    │ Business + Security │
                    └──────────┬──────────┘
                               │
                               ▼
                    ┌─────────────────────┐
                    │    Repositories     │
                    │  Spring Data JPA    │
                    └──────────┬──────────┘
                               │
                               ▼
                    ┌─────────────────────┐
                    │   MySQL Database    │
                    └─────────────────────┘
```

Additional cross-cutting components provide:

- JWT authentication
- Authorization
- DTO validation
- Exception handling
- API documentation
- Transaction management

### Package Structure

```text
src/main/java/com/example/banking_application/
│
├── config/          # Application and OpenAPI configuration
├── controller/      # REST controllers
├── dto/             # Request and response DTOs
├── exception/       # Domain exceptions and global error handling
├── model/           # JPA entities and enums
├── repository/      # Spring Data JPA repositories
├── security/        # JWT authentication and security components
├── service/         # Business logic and authorization
│
└── BankingApplication.java
```

---

## Security

Security is implemented using **Spring Security and JWT** with a stateless authentication model.

### Authentication Flow

```text
Login Request
     │
     ▼
Verify Email + Password
     │
     ▼
Generate JWT
     │
     ▼
Client sends:
Authorization: Bearer <JWT>
     │
     ▼
JWT Authentication Filter
     │
     ▼
SecurityContext
     │
     ▼
Role Authorization
     │
     ▼
Resource Ownership Validation
     │
     ▼
Protected Operation
```

### Security Features

- Stateless JWT authentication
- BCrypt password hashing
- `USER` and `ADMIN` roles
- URL-level security rules
- Method-level authorization
- Resource ownership validation
- IDOR protection
- Generic invalid-credential responses
- Case-insensitive email authentication
- Externalized JWT secret
- Custom `401 Unauthorized` responses
- Custom `403 Forbidden` responses

### Authorization Model

#### USER

Authenticated users can perform banking operations on resources they own, including:

- Create an account
- View their own account
- Deposit funds
- Withdraw funds
- Transfer funds from their account
- View their transaction history

A user cannot perform protected operations on another user's account.

#### ADMIN

Administrators can perform privileged account-management operations, including:

- View all accounts
- Access account information administratively
- Update account types
- Change account lifecycle status
- Access ADMIN-protected endpoints

Public registration always creates a `USER`. Administrative privileges are not granted through the registration API.

---

## Account Model

Each account belongs to a user and has an account type, balance, and lifecycle status.

### Account Types

```text
SAVINGS
CURRENT
```

### Account Status

```text
ACTIVE
BLOCKED
CLOSED
```

New accounts are created with:

```text
Balance: 0
Status:  ACTIVE
```

### Lifecycle Rules

```text
ACTIVE ─────────────► BLOCKED
   │                    │
   │                    │
   ▼                    ▼
 CLOSED ◄──────────── CLOSED
```

Key rules:

- A `CLOSED` account cannot be reopened.
- An account must have a zero balance before closure.
- Financial operations require an appropriate operational account state.
- Account ownership cannot be changed through the API.
- Account numbers cannot be changed after creation.
- Balances cannot be directly edited through account-management APIs.

---

## Transaction Model

The application maintains transaction records for balance-changing operations.

### Transaction Types

```text
DEPOSIT
WITHDRAWAL
TRANSFER_OUT
TRANSFER_IN
```

### Deposits

A deposit:

1. Verifies account ownership.
2. Locks the account.
3. Validates account state.
4. Updates the balance.
5. Creates a `DEPOSIT` transaction record.

### Withdrawals

A withdrawal:

1. Verifies account ownership.
2. Locks the account.
3. Validates account state.
4. Checks available balance.
5. Updates the balance.
6. Creates a `WITHDRAWAL` transaction record.

### Transfers

Transfers are executed transactionally.

```text
Sender Account
      │
      │ TRANSFER_OUT
      ▼
 ┌─────────┐
 │ Transfer│
 └─────────┘
      │
      │ TRANSFER_IN
      ▼
Receiver Account
```

A transfer:

1. Verifies ownership of the sender account.
2. Validates the sender and receiver.
3. Locks both accounts in deterministic ID order.
4. Validates both account states.
5. Checks the sender's available balance.
6. Debits the sender.
7. Credits the receiver.
8. Records `TRANSFER_OUT`.
9. Records `TRANSFER_IN`.

Deterministic lock ordering helps reduce the risk of database deadlocks during concurrent transfers.

Pessimistic locking protects critical balance-changing operations from concurrent modification.

---

## API Endpoints

### Authentication & Users

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/api/users` | Public | Register a new user |
| `POST` | `/api/auth/login` | Public | Authenticate and receive JWT |
| `GET` | `/api/auth/admin/test` | ADMIN | Verify administrator access |

### Accounts

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/api/accounts` | USER / ADMIN | Create an account |
| `GET` | `/api/accounts/{id}` | Owner / ADMIN | Get account details |
| `GET` | `/api/accounts` | ADMIN | Get all accounts |
| `PUT` | `/api/accounts/{id}` | ADMIN | Update account type |
| `PATCH` | `/api/accounts/{id}/status` | ADMIN | Update account status |

### Transactions

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/api/transactions/{id}/deposit` | Owner | Deposit funds |
| `POST` | `/api/transactions/{id}/withdraw` | Owner | Withdraw funds |
| `POST` | `/api/transactions/{id}/transfer` | Sender owner | Transfer funds |
| `GET` | `/api/transactions/{id}/records` | Owner | Get transaction history |

Transaction history supports pagination, sorting, and optional transaction-type filtering.

Example:

```http
GET /api/transactions/1/records?page=0&size=10&sort=transactionDate,desc
```

---

## API Documentation

The API is documented using **OpenAPI 3 and Swagger UI**.

After starting the application:

### Swagger UI

```text
http://localhost:8080/swagger-ui.html
```

### OpenAPI Specification

```text
http://localhost:8080/v3/api-docs
```

The generated documentation includes:

- Endpoint descriptions
- Request schemas
- DTO field descriptions
- Example values
- Enum values
- HTTP response codes
- Pagination parameters
- JWT Bearer authentication

### Authenticating in Swagger

1. Register a user through `/api/users`.
2. Login through `/api/auth/login`.
3. Copy the returned JWT.
4. Click **Authorize** in Swagger UI.
5. Paste the raw JWT.
6. Execute protected endpoints.

Swagger automatically sends:

```http
Authorization: Bearer <JWT>
```

---

## Getting Started

### Prerequisites

Make sure the following are available:

- Java 21+
- MySQL
- Git

The Maven Wrapper is included, so installing Maven separately is optional.

### Clone the Repository

```bash
git clone https://github.com/Abhishek-Savita-3012/spring-boot-banking-application.git
cd spring-boot-banking-application
```

### Create the Database

Create a MySQL database:

```sql
CREATE DATABASE banking_db;
```

### Configure Environment Variables

The application does not require database credentials or JWT secrets to be committed to source control.

The expected configuration is:

```properties
spring.datasource.url=${DB_URL:jdbc:mysql://localhost:3306/banking_db}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}

jwt.secret=${JWT_SECRET}
jwt.expiration-ms=${JWT_EXPIRATION_MS:3600000}
```

Example PowerShell configuration:

```powershell
$env:DB_USERNAME="your_mysql_username"
$env:DB_PASSWORD="your_mysql_password"
$env:JWT_SECRET="your_long_random_jwt_secret"
```

Optional variables:

```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/banking_db"
$env:JWT_EXPIRATION_MS="3600000"
```

> Never commit real database credentials, JWT secrets, or `.env` files.

### Run the Application

Windows:

```powershell
.\mvnw spring-boot:run
```

Linux/macOS:

```bash
./mvnw spring-boot:run
```

The API is available by default at:

```text
http://localhost:8080
```

---

## Testing

The project contains a comprehensive automated test suite covering business logic, persistence integration, HTTP behavior, authentication, authorization, validation, and error handling.

### Test Strategy

```text
                    Automated Tests
                          │
             ┌────────────┴────────────┐
             │                         │
             ▼                         ▼
        Unit Tests              Integration Tests
             │                         │
       JUnit + Mockito        SpringBootTest + MockMvc
             │                         │
      Service Isolation        Full Spring Context
                                       │
                                       ▼
                                H2 Test Database
```

### Unit Tests

Service-layer unit tests use **JUnit 5 and Mockito**.

Covered components include:

- `UserService`
- `AuthService`
- `AccountService`
- `TransactionService`

Unit tests verify business rules without starting the complete Spring application context.

### Integration & Security Tests

Integration tests use:

- `@SpringBootTest`
- MockMvc
- H2 in-memory database
- Dedicated `test` profile

They verify:

- User registration
- Authentication
- JWT processing
- USER/ADMIN authorization
- `401` vs `403` behavior
- Account ownership
- IDOR protection
- Account creation and updates
- Account lifecycle rules
- Deposits
- Withdrawals
- Transfers
- Insufficient balance handling
- Transaction history
- Pagination and filtering
- DTO validation
- Malformed requests
- API error responses

### Current Test Results

```text
Unit Tests                    24 / 24
Integration & Security Tests  55 / 55
─────────────────────────────────────
Total                         79 / 79

Failures                       0
Errors                         0
Skipped                        0

BUILD SUCCESS
```

Run the complete test suite:

**Windows**

```powershell
.\mvnw test
```

**Linux/macOS**

```bash
./mvnw test
```

---

## Error Handling

The application uses centralized exception handling to provide consistent API responses.

| HTTP Status | Usage |
|---|---|
| `200 OK` | Successful operation |
| `400 Bad Request` | Validation failure or malformed request |
| `401 Unauthorized` | Missing or invalid authentication |
| `403 Forbidden` | Authenticated user lacks permission |
| `404 Not Found` | Requested resource does not exist |
| `409 Conflict` | Duplicate resource or invalid business-state transition |
| `500 Internal Server Error` | Unexpected server-side failure |

Unexpected internal exceptions are logged server-side while clients receive a generic error response.

---

## Business & Security Rules

Some of the important rules enforced by the backend are:

- New users receive the `USER` role by default.
- Emails are normalized for case-insensitive authentication and duplicate detection.
- Passwords are stored using secure password hashing.
- New accounts start with zero balance and `ACTIVE` status.
- Users can operate only on accounts they own.
- Transfers require ownership of the source account, not the receiver account.
- Account numbers and owners are immutable.
- Account balances cannot be directly modified through account update endpoints.
- Balance changes occur through ledgered transactions.
- Blocked or closed accounts cannot perform restricted financial operations.
- Accounts must have zero balance before closure.
- Closed accounts cannot be reopened.
- Account closure does not delete transaction history.
- Transfer operations produce separate outgoing and incoming ledger records.
- Critical account updates use pessimistic locking.
- Transfers acquire locks in deterministic order to reduce deadlock risk.

---

## Design Decisions

### Why JWT?

JWT enables stateless authentication, allowing the backend to authenticate API requests without maintaining server-side HTTP sessions.

### Why DTOs?

DTOs prevent persistence entities from becoming the public API contract and provide dedicated models for validation and responses.

### Why Resource Ownership Checks?

Role checks alone are insufficient. Two users can both have the `USER` role, but one user must not be able to access another user's account by changing an account ID in the request.

Resource ownership validation protects against this class of IDOR vulnerability.

### Why Pessimistic Locking?

Bank balances are shared mutable financial state. Concurrent requests could otherwise read and update the same balance simultaneously.

Pessimistic locking serializes critical account modifications at the database level.

### Why Separate `TRANSFER_OUT` and `TRANSFER_IN` Records?

A transfer affects two accounts. Separate ledger records make each account's transaction history accurately represent its side of the transfer.

---

## Project Status

The current backend includes:

- [x] REST API architecture
- [x] User registration
- [x] JWT authentication
- [x] Role-based authorization
- [x] Resource ownership authorization
- [x] Account lifecycle management
- [x] Deposits and withdrawals
- [x] Account-to-account transfers
- [x] Transaction ledger
- [x] Pagination and filtering
- [x] Centralized exception handling
- [x] Unit testing
- [x] Integration testing
- [x] Security testing
- [x] OpenAPI documentation
- [x] Swagger UI
- [x] Externalized secrets

---

## Roadmap

Potential future improvements include:

- Refresh-token support
- Email verification
- Password reset workflow
- Audit logging
- Rate limiting
- Flyway or Liquibase database migrations
- Docker containerization
- CI/CD with automated test execution
- Cloud deployment
- Monitoring and observability
- Frontend banking dashboard

---

## Repository

**GitHub:**  
`https://github.com/Abhishek-Savita-3012/spring-boot-banking-application`

---

## Author

**Abhishek Savita**

GitHub: `Abhishek-Savita-3012`

---

## Disclaimer

This project is intended for **learning, demonstration, and portfolio purposes**.

It is not a production banking platform and should not be used to process real financial transactions or store real banking credentials.