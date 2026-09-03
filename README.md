# Spring Boot Banking Application

A secure RESTful banking backend built with **Java, Spring Boot, Spring Security, Spring Data JPA, MySQL, JWT, and OpenAPI/Swagger**.

The application provides user registration and authentication, role-based authorization, account management, deposits, withdrawals, transfers, transaction history, account lifecycle management, and resource ownership protection.

The project also includes a comprehensive automated test suite covering service-layer logic, API integration, authentication, authorization, validation, error handling, and banking business rules.

---

## Features

### Authentication & Security

- User registration and login
- JWT-based stateless authentication
- Password hashing using Spring Security
- Role-based access control with `USER` and `ADMIN` roles
- Method-level authorization
- Account ownership validation
- Protection against IDOR-style unauthorized resource access
- Consistent `401 Unauthorized` and `403 Forbidden` handling
- JWT secret externalized through environment variables

### Account Management

- Create bank accounts
- View account details
- Admin access to all accounts
- Update account type
- Account types:
    - `SAVINGS`
    - `CURRENT`
- Account lifecycle:
    - `ACTIVE`
    - `BLOCKED`
    - `CLOSED`
- New accounts start with zero balance and `ACTIVE` status
- Account numbers and account owners are immutable
- Closed accounts cannot be reopened
- An account must have zero balance before it can be closed

### Transactions

- Deposit money
- Withdraw money
- Transfer money between accounts
- Transaction history
- Pagination and sorting support
- Optional transaction-type filtering
- Transaction types:
    - `DEPOSIT`
    - `WITHDRAWAL`
    - `TRANSFER_OUT`
    - `TRANSFER_IN`
- Insufficient-balance validation
- Transactions allowed only for valid account states
- Transfer operations create separate sender and receiver ledger entries
- Deterministic account locking helps reduce transfer deadlock risk
- Pessimistic locking protects critical balance updates

### Validation & Error Handling

- Request validation using Jakarta Bean Validation
- Duplicate email detection
- Duplicate account-number detection
- Invalid account-state handling
- Invalid credentials handling
- Malformed request handling
- Centralized exception handling
- Production-safe error responses and logging

---

## Tech Stack

| Technology | Purpose |
|---|---|
| Java 21 | Programming language |
| Spring Boot 4 | Application framework |
| Spring Web MVC | REST API development |
| Spring Security | Authentication and authorization |
| Spring Data JPA | Persistence layer |
| Hibernate | ORM |
| MySQL | Main relational database |
| H2 | Isolated integration-test database |
| JWT (JJWT) | Stateless authentication |
| Jakarta Validation | Request validation |
| OpenAPI 3 | API specification |
| Swagger UI | Interactive API documentation |
| JUnit 5 | Automated testing |
| Mockito | Unit testing and mocking |
| MockMvc | HTTP integration/security testing |
| Maven | Dependency management and build tool |

---

## Architecture

The application follows a layered backend architecture:

```text
Client
  │
  ▼
Controller
  │
  ▼
Service
  │
  ▼
Repository
  │
  ▼
Database
```

Supporting layers provide authentication, authorization, DTO mapping, validation, exception handling, and API documentation.

```text
com.example.banking_application
│
├── config
├── controller
├── dto
├── exception
├── model
├── repository
├── security
├── service
└── BankingApplication.java
```

### Layer Responsibilities

**Controller Layer**

Handles HTTP requests, request validation, response status codes, and API endpoints.

**Service Layer**

Contains banking business logic, authorization checks, account lifecycle rules, transaction handling, and transfer coordination.

**Repository Layer**

Uses Spring Data JPA to communicate with the relational database.

**Security Layer**

Handles JWT validation, authenticated principals, Spring Security configuration, role-based authorization, and resource ownership protection.

**DTO Layer**

Separates API request/response models from persistence entities.

**Exception Layer**

Provides domain-specific exceptions and centralized API error handling.

---

## Security Model

The application uses stateless JWT authentication.

```text
User Login
    │
    ▼
Credentials Verified
    │
    ▼
JWT Generated
    │
    ▼
Client Sends Bearer Token
    │
    ▼
JwtAuthenticationFilter
    │
    ▼
SecurityContext
    │
    ▼
Role Authorization
    │
    ▼
Resource Ownership Check
    │
    ▼
Protected Banking Operation
```

### Roles

#### USER

A normal user can:

- Create an account
- View their own accounts
- Deposit into their own account
- Withdraw from their own account
- Transfer from their own account
- View their own transaction history

A USER cannot access another user's protected account resources.

#### ADMIN

An administrator can perform privileged account-management operations, including:

- View all accounts
- Access accounts administratively
- Update account types
- Change account status
- Access ADMIN-protected endpoints

Administrative privileges are not granted through public user registration.

---

## Account Lifecycle

Accounts follow controlled lifecycle rules:

```text
ACTIVE ──────► BLOCKED
   │             │
   │             ▼
   └──────────► CLOSED
```

A `CLOSED` account is terminal and cannot be reopened.

Closing an account requires a zero balance.

Financial operations such as deposits, withdrawals, and transfers require accounts to be in an allowed operational state.

---

## Transfer Safety

Transfers are handled as atomic banking operations.

A successful transfer:

1. Validates the sender's ownership.
2. Validates both accounts.
3. Locks involved accounts in deterministic ID order.
4. Checks account status.
5. Checks available balance.
6. Debits the sender.
7. Credits the receiver.
8. Records a `TRANSFER_OUT` transaction.
9. Records a `TRANSFER_IN` transaction.

The deterministic lock order helps reduce the possibility of database deadlocks during concurrent transfers.

---

## API Documentation

Interactive API documentation is available through **Swagger UI**.

After starting the application, open:

```text
http://localhost:8080/swagger-ui.html
```

The generated OpenAPI specification is available at:

```text
http://localhost:8080/v3/api-docs
```

Swagger documentation includes:

- Endpoint descriptions
- Request schemas
- Example values
- Response status codes
- Enum values
- Pagination parameters
- JWT Bearer authentication

### Using JWT in Swagger

1. Register a user.
2. Login through `/api/auth/login`.
3. Copy the returned JWT.
4. Click **Authorize** in Swagger UI.
5. Enter the raw JWT token.
6. Execute protected endpoints.

Swagger automatically sends the token using the Bearer authentication scheme.

---

## Main API Endpoints

### Users

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/users` | Register a new user |

### Authentication

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/auth/login` | Authenticate and receive JWT |
| GET | `/api/auth/admin/test` | Test ADMIN authorization |

### Accounts

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/accounts` | Create an account |
| GET | `/api/accounts/{id}` | Get account by ID |
| GET | `/api/accounts` | Get all accounts (ADMIN) |
| PUT | `/api/accounts/{id}` | Update account type (ADMIN) |
| PATCH | `/api/accounts/{id}/status` | Update account status (ADMIN) |

### Transactions

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/transactions/{id}/deposit` | Deposit money |
| POST | `/api/transactions/{id}/withdraw` | Withdraw money |
| POST | `/api/transactions/{id}/transfer` | Transfer money |
| GET | `/api/transactions/{id}/records` | Get transaction history |

Transaction history supports pagination, sorting, and optional transaction-type filtering.

Example:

```text
/api/transactions/1/records?page=0&size=10&sort=transactionDate,desc
```

---

## Environment Configuration

The application requires database configuration and a JWT signing secret.

Do **not** commit real passwords or secrets to Git.

Example configuration:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/banking_db
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}

jwt.secret=${JWT_SECRET}
jwt.expiration-ms=${JWT_EXPIRATION_MS:3600000}
```

Set environment variables before starting the application.

Example using PowerShell:

```powershell
$env:DB_USERNAME="your_database_username"
$env:DB_PASSWORD="your_database_password"
$env:JWT_SECRET="your_secure_jwt_secret"
```

Use a sufficiently strong JWT signing secret.

---

## Database Setup

Create a MySQL database:

```sql
CREATE DATABASE banking_db;
```

Configure the application to connect to the database using environment variables.

Spring Data JPA and Hibernate manage persistence according to the application's configured JPA settings.

---

## Running the Application

### Prerequisites

Install:

- Java 21
- MySQL
- Git

The project includes the Maven Wrapper, so a separate Maven installation is not required.

### Clone the Repository

```bash
git clone https://github.com/Abhishek-Savita-3012/spring-boot-banking-application.git
cd spring-boot-banking-application
```

### Configure Environment Variables

Set the required database credentials and JWT secret.

### Run with Maven Wrapper

On Windows PowerShell:

```powershell
.\mvnw spring-boot:run
```

On Linux/macOS:

```bash
./mvnw spring-boot:run
```

The application runs by default at:

```text
http://localhost:8080
```

---

## Automated Testing

The project contains both unit and integration tests.

### Unit Tests

Service-layer tests use:

- JUnit 5
- Mockito

They test business logic in isolation without starting the complete Spring application context.

Covered services include:

- `UserService`
- `AuthService`
- `AccountService`
- `TransactionService`

### Integration & Security Tests

Integration tests use:

- `@SpringBootTest`
- MockMvc
- H2 in-memory database
- Test profile configuration

They verify:

- User registration
- Authentication
- JWT security
- USER and ADMIN authorization
- Account ownership
- IDOR protection
- Account operations
- Deposits and withdrawals
- Transfers
- Transaction history
- Pagination and filtering
- Request validation
- API error handling
- Account lifecycle rules

### Current Test Suite

```text
Unit Tests                   24
Integration/Security Tests   55
--------------------------------
Total Tests                  79

Failures                      0
Errors                        0
```

Run all tests with:

```powershell
.\mvnw test
```

Expected result:

```text
Tests run: 79, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

---

## Important Business Rules

- Every newly registered user receives the `USER` role by default.
- User email addresses are normalized for case-insensitive authentication and duplicate detection.
- Passwords are never stored as plain text.
- New accounts start with a zero balance.
- New accounts start with `ACTIVE` status.
- Account ownership cannot be changed through the API.
- Account numbers cannot be changed after creation.
- Account balance cannot be directly edited through account-update APIs.
- Balance changes occur through ledgered banking transactions.
- Users can operate only on accounts they own.
- Transfers may send funds to another user's account, but the sender must own the source account.
- Closed accounts cannot be reopened.
- Accounts must have zero balance before closure.
- Transaction records are retained when an account is closed.

---

## API Error Semantics

The API uses standard HTTP status codes, including:

| Status | Meaning |
|---|---|
| `200 OK` | Request completed successfully |
| `400 Bad Request` | Invalid request or validation failure |
| `401 Unauthorized` | Authentication is required or invalid |
| `403 Forbidden` | Authenticated user lacks required permission |
| `404 Not Found` | Requested resource does not exist |
| `409 Conflict` | Business-state conflict or duplicate resource |
| `500 Internal Server Error` | Unexpected server-side failure |

---

## Project Highlights

This project demonstrates practical backend engineering concepts including:

- REST API design
- Layered Spring Boot architecture
- JWT authentication
- Role-based access control
- Method-level security
- Resource-level authorization
- IDOR protection
- Secure password storage
- DTO-based API design
- Bean Validation
- Centralized exception handling
- Transactional banking operations
- Pessimistic database locking
- Deadlock-risk reduction
- Immutable financial attributes
- Ledger-style transaction records
- Pagination and filtering
- Unit testing with Mockito
- Integration testing with MockMvc
- Isolated H2 test database
- OpenAPI 3 documentation
- Interactive Swagger UI

---

## Future Improvements

Potential future enhancements include:

- Refresh tokens
- Email verification
- Password reset workflow
- Audit logging
- Rate limiting
- Database migrations using Flyway or Liquibase
- Docker containerization
- CI/CD pipeline
- Cloud deployment
- Frontend banking dashboard

---

## Author

**Abhishek Savita**

GitHub: `Abhishek-Savita-3012`

---

## Disclaimer

This application is an educational and portfolio backend project. It is not intended to process real financial transactions or store real banking credentials.