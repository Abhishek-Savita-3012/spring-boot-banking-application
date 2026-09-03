# Spring Boot Banking Application

A secure, containerized REST API for core banking operations, built with **Java 21, Spring Boot 4, Spring Security, Spring Data JPA, JWT, MySQL, Docker, OpenAPI, and GitHub Actions**.

The project demonstrates backend engineering practices beyond basic CRUD, including stateless authentication, role- and resource-level authorization, account lifecycle management, transactional money transfers, pessimistic locking, persistent containerized infrastructure, automated testing, health monitoring, and continuous integration.

---

## Overview

The Spring Boot Banking Application is a RESTful backend for managing users, bank accounts, and financial transactions.

It provides:

- User registration and authentication
- JWT-based stateless security
- `USER` and `ADMIN` authorization
- Account ownership and IDOR protection
- Savings and current accounts
- Account lifecycle management
- Deposits and withdrawals
- Account-to-account transfers
- Transaction history with pagination and filtering
- OpenAPI 3 / Swagger UI documentation
- Spring Boot Actuator health monitoring
- Dockerized application and MySQL services
- Persistent MySQL storage using Docker volumes
- Unit, integration, API, and security testing
- Automated CI using GitHub Actions

The application follows a layered architecture that separates HTTP handling, business logic, security, persistence, validation, and API contracts.

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
| Database | MySQL 8.4 |
| Test Database | H2 |
| Validation | Jakarta Bean Validation |
| API Documentation | OpenAPI 3 / Swagger UI |
| OpenAPI Integration | springdoc-openapi |
| Health Monitoring | Spring Boot Actuator |
| Unit Testing | JUnit 5, Mockito |
| Integration Testing | Spring Boot Test, MockMvc |
| Containerization | Docker |
| Service Orchestration | Docker Compose |
| CI | GitHub Actions |
| Build Tool | Maven |
| Version Control | Git / GitHub |

---

## Architecture

### Application Architecture

```text
                     Client / Swagger UI
                             │
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

Cross-cutting components provide:

- JWT authentication
- Role-based authorization
- Resource ownership authorization
- DTO validation
- Transaction management
- Centralized exception handling
- OpenAPI documentation
- Health monitoring

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

### Containerized Architecture

```text
                         Host Machine
                              │
                              │ :8080
                              ▼
                  ┌─────────────────────────┐
                  │   Banking API Container │
                  │                         │
                  │ Spring Boot + Java 21   │
                  │ Spring Security + JWT   │
                  │ Spring Data JPA         │
                  └────────────┬────────────┘
                               │
                               │ Docker Network
                               │ jdbc:mysql://mysql:3306/banking_db
                               ▼
                  ┌─────────────────────────┐
                  │     MySQL Container     │
                  │        MySQL 8.4        │
                  └────────────┬────────────┘
                               │
                               ▼
                  ┌─────────────────────────┐
                  │ Persistent Named Volume │
                  │   banking_mysql_data    │
                  └─────────────────────────┘
```

Docker Compose creates an isolated network where the application connects to MySQL using the service name `mysql` instead of `localhost`.

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
- Externalized JWT signing secret
- Custom `401 Unauthorized` responses
- Custom `403 Forbidden` responses
- No public API for assigning the `ADMIN` role
- Environment-based sensitive configuration

### Authorization Model

#### USER

Authenticated users can:

- Create bank accounts
- View their own accounts
- Deposit funds into their accounts
- Withdraw funds from their accounts
- Transfer funds from their accounts
- View their transaction history

A `USER` cannot access another user's protected account resources.

#### ADMIN

Administrators can:

- View all accounts
- Access account information administratively
- Update account types
- Change account lifecycle status
- Access ADMIN-protected endpoints

Public registration always creates a `USER`. Administrative privileges are not granted through the registration API.

---

## Account Model

Each account belongs to a user and contains an account number, account type, balance, and lifecycle status.

### Account Types

```text
SAVINGS
CURRENT
```

### Account Statuses

```text
ACTIVE
BLOCKED
CLOSED
```

New accounts start with:

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

Important rules:

- A `CLOSED` account cannot be reopened.
- An account must have zero balance before closure.
- Restricted financial operations require an operational account state.
- Account ownership cannot be changed through the API.
- Account numbers cannot be changed after creation.
- Account balances cannot be directly edited through account-management endpoints.
- Closing an account preserves its transaction history.

---

## Transaction Model

The application maintains ledger records for every balance-changing operation.

### Transaction Types

```text
DEPOSIT
WITHDRAWAL
TRANSFER_OUT
TRANSFER_IN
```

### Deposit Flow

A deposit:

1. Verifies account ownership.
2. Acquires a pessimistic database lock.
3. Validates account state.
4. Updates the balance.
5. Records a `DEPOSIT` transaction.

### Withdrawal Flow

A withdrawal:

1. Verifies account ownership.
2. Acquires a pessimistic database lock.
3. Validates account state.
4. Checks available balance.
5. Updates the balance.
6. Records a `WITHDRAWAL` transaction.

### Transfer Flow

Transfers execute transactionally:

```text
Sender Account
      │
      │ TRANSFER_OUT
      ▼
 ┌──────────┐
 │ Transfer │
 └──────────┘
      │
      │ TRANSFER_IN
      ▼
Receiver Account
```

A transfer:

1. Verifies ownership of the sender account.
2. Validates sender and receiver accounts.
3. Locks both accounts in deterministic ID order.
4. Validates both account states.
5. Checks the sender's available balance.
6. Debits the sender.
7. Credits the receiver.
8. Records `TRANSFER_OUT`.
9. Records `TRANSFER_IN`.

Pessimistic locking protects critical balance-changing operations from concurrent modification.

Deterministic lock ordering helps reduce the risk of database deadlocks during concurrent transfers.

---

## API Endpoints

### Authentication & Users

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/api/users` | Public | Register a user |
| `POST` | `/api/auth/login` | Public | Authenticate and receive a JWT |
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

### Swagger UI

After starting the application:

```text
http://localhost:8080/swagger-ui.html
```

### OpenAPI Specification

```text
http://localhost:8080/v3/api-docs
```

The generated API documentation includes:

- Endpoint descriptions
- Request and response schemas
- DTO field descriptions
- Example values
- Enum values
- HTTP response codes
- Pagination parameters
- JWT Bearer authentication

### Authenticating in Swagger

1. Register through `POST /api/users`.
2. Login through `POST /api/auth/login`.
3. Copy the returned JWT.
4. Click **Authorize** in Swagger UI.
5. Paste the raw JWT.
6. Execute protected endpoints.

Swagger sends:

```http
Authorization: Bearer <JWT>
```

---

## Running with Docker

Docker Compose is the recommended way to run the complete application because it starts both the Spring Boot API and MySQL in an isolated environment.

### Prerequisites

Install:

- Git
- Docker Desktop
- Docker Compose

Verify Docker:

```bash
docker version
docker compose version
```

### Clone the Repository

```bash
git clone https://github.com/Abhishek-Savita-3012/spring-boot-banking-application.git
cd spring-boot-banking-application
```

### Configure Docker Environment Variables

Create a `.env` file in the project root:

```env
MYSQL_PASSWORD=your_banking_database_password
MYSQL_ROOT_PASSWORD=your_mysql_root_password
JWT_SECRET=your_long_secure_jwt_secret
```

Use strong values instead of the placeholders.

The `.env` file is ignored by Git and must never be committed.

### Build the Application Image

```bash
docker build -t banking-application:latest .
```

Verify the image:

```bash
docker images
```

### Validate Docker Compose

```bash
docker compose config
```

> `docker compose config` can display resolved environment-variable values. Avoid sharing its output when it contains secrets.

### Start the Complete Stack

```bash
docker compose up -d
```

Verify the containers:

```bash
docker compose ps
```

The expected services are:

```text
banking-api      Up
banking-mysql    Up (healthy)
```

The API is available at:

```text
http://localhost:8080
```

Swagger UI:

```text
http://localhost:8080/swagger-ui.html
```

Health endpoint:

```text
http://localhost:8080/actuator/health
```

### Stop the Stack

```bash
docker compose down
```

The containers are removed, but MySQL data remains stored in the named Docker volume.

Inspect volumes with:

```bash
docker volume ls
```

> Do not use `docker compose down -v` unless you intentionally want to remove the persistent MySQL volume and its data.

---

## Running without Docker

The application can also run directly against a local MySQL installation.

### Prerequisites

- Java 21+
- MySQL
- Git

The Maven Wrapper is included, so a separate Maven installation is optional.

### Create the Database

```sql
CREATE DATABASE banking_db;
```

### Configure Environment Variables

The application expects sensitive configuration through environment variables.

Example configuration:

```properties
spring.datasource.url=${DB_URL:jdbc:mysql://localhost:3306/banking_db}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}

jwt.secret=${JWT_SECRET}
jwt.expiration-ms=${JWT_EXPIRATION_MS:3600000}
```

Example PowerShell environment:

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

Start the application on Windows:

```powershell
.\mvnw spring-boot:run
```

Linux/macOS:

```bash
./mvnw spring-boot:run
```

The API starts by default on:

```text
http://localhost:8080
```

---

## Persistent Database Storage

The Docker environment uses a named volume for MySQL data.

```text
MySQL Container
      │
      ▼
banking_mysql_data
```

This separates database state from the lifecycle of an individual container.

As a result:

```text
docker compose down
        │
        ▼
Containers removed
        │
        ▼
Named volume preserved
        │
        ▼
docker compose up -d
        │
        ▼
Containers recreated
        │
        ▼
Existing database data remains available
```

The persistence behavior has been verified by creating users, accounts, and transactions, removing the containers, recreating the stack, and confirming that the stored data remains available.

---

## Health Monitoring

Spring Boot Actuator provides an operational health endpoint:

```http
GET /actuator/health
```

Example:

```json
{
  "status": "UP"
}
```

Depending on the configured Actuator health groups, the response may also expose liveness and readiness group names.

The health endpoint allows container platforms, deployment systems, and monitoring tools to determine whether the application is available without invoking a business API.

Only selected Actuator functionality is exposed publicly.

---

## Automated Testing

The project contains **79 automated tests** covering business logic, persistence integration, HTTP behavior, authentication, authorization, validation, and error handling.

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

### Unit Tests — 24

Service-layer unit tests use **JUnit 5 and Mockito** without starting the complete Spring application context.

Covered components:

- `UserService`
- `AuthService`
- `AccountService`
- `TransactionService`

Coverage includes:

- User registration
- Email normalization
- Duplicate email detection
- Authentication
- Account creation
- Account lifecycle rules
- Deposits
- Withdrawals
- Insufficient balance handling
- Transfers
- Transfer ledger creation
- Deterministic account locking
- Transaction history

### Integration & Security Tests — 55

Integration tests use:

- `@SpringBootTest`
- MockMvc
- Spring Security
- H2 in-memory database
- Dedicated `test` profile

Coverage includes:

- User registration
- Authentication
- JWT processing
- USER/ADMIN authorization
- `401` vs `403` behavior
- Account ownership
- IDOR protection
- Account APIs
- Account lifecycle rules
- Transaction APIs
- Deposits and withdrawals
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

Run all tests on Windows:

```powershell
.\mvnw test
```

Linux/macOS:

```bash
./mvnw test
```

---

## Continuous Integration

The repository includes a GitHub Actions workflow:

```text
.github/workflows/ci.yml
```

The CI workflow runs automatically on:

- Pushes to `main`
- Pull requests targeting `main`

### CI Pipeline

```text
Push / Pull Request
        │
        ▼
GitHub Actions
        │
        ▼
Fresh Ubuntu Runner
        │
        ├── Checkout Repository
        │
        ├── Configure Java 21
        │
        ├── Cache Maven Dependencies
        │
        └── Run Automated Tests
                     │
                     ▼
                 79 Tests
                  │     │
                  ▼     ▼
                PASS   FAIL
                  │
                  ▼
             CI Success
```

The pipeline verifies the project independently from the developer's local environment.

Integration tests use the H2 test database, so the CI workflow does not require production MySQL credentials.

---

## Error Handling

The application uses centralized exception handling to provide consistent HTTP responses.

| HTTP Status | Usage |
|---|---|
| `200 OK` | Successful operation |
| `201 Created` | Resource created successfully |
| `400 Bad Request` | Validation failure or malformed request |
| `401 Unauthorized` | Missing or invalid authentication |
| `403 Forbidden` | Authenticated user lacks permission |
| `404 Not Found` | Requested resource does not exist |
| `409 Conflict` | Duplicate resource or invalid business-state transition |
| `500 Internal Server Error` | Unexpected server-side failure |

Unexpected internal exceptions are logged server-side while clients receive a generic error response.

---

## Business & Security Rules

Important rules enforced by the backend include:

- New users receive the `USER` role by default.
- Emails are normalized for case-insensitive authentication and duplicate detection.
- Passwords are stored using secure password hashing.
- New accounts start with zero balance and `ACTIVE` status.
- Users can operate only on accounts they own.
- Transfers require ownership of the source account, not the receiver account.
- Account numbers and owners are immutable.
- Account balances cannot be directly modified through account-management APIs.
- Balance changes occur only through ledgered financial operations.
- Blocked or closed accounts cannot perform restricted financial operations.
- Accounts must have zero balance before closure.
- Closed accounts cannot be reopened.
- Account closure preserves transaction history.
- Transfers produce separate outgoing and incoming ledger records.
- Critical account operations use pessimistic database locking.
- Transfers acquire locks in deterministic order to reduce deadlock risk.

---

## Design Decisions

### Why JWT?

JWT enables stateless authentication, allowing protected requests to be authenticated without maintaining server-side HTTP sessions.

### Why DTOs?

DTOs separate the persistence model from the public API contract and provide dedicated request/response models for validation and controlled data exposure.

### Why Resource Ownership Checks?

Role authorization alone is insufficient.

Two users may both have the `USER` role, but one user must not gain access to another user's account simply by changing an account ID in a request.

Resource-level ownership validation protects against this class of IDOR vulnerability.

### Why Pessimistic Locking?

Account balances are shared mutable financial state.

Concurrent requests could otherwise read and update the same balance simultaneously. Pessimistic database locking serializes critical balance-changing operations.

### Why Deterministic Lock Ordering?

Transfers modify two accounts.

Acquiring both account locks in a consistent ID order reduces the possibility of concurrent transfers acquiring the same locks in opposite order and deadlocking.

### Why Separate `TRANSFER_OUT` and `TRANSFER_IN` Records?

A transfer affects two accounts.

Separate ledger records allow each account's transaction history to accurately represent its side of the transfer.

### Why Docker Compose?

The application depends on both Spring Boot and MySQL.

Docker Compose provides a reproducible multi-container environment, internal service networking, environment-based configuration, startup dependency management, and persistent database storage.

### Why H2 for Integration Tests?

Integration tests need a fast, isolated, disposable database.

Using an H2 in-memory database through a dedicated Spring test profile allows the full test suite to run locally and in GitHub Actions without requiring production database credentials.

---

## Configuration & Secrets

Sensitive configuration is externalized through environment variables.

The repository should not contain:

- Database passwords
- MySQL root passwords
- JWT signing secrets
- Local `.env` files

Local Docker secrets are supplied through a Git-ignored `.env` file.

For direct local execution, configuration can be provided through operating-system environment variables or IDE run configuration.

Example configuration files should contain placeholders only.

> If a real credential has ever been committed to Git history, removing it from the current file does not invalidate that credential. Rotate exposed credentials.

---

## Project Highlights

- Layered Spring Boot REST architecture
- Stateless JWT authentication
- BCrypt password hashing
- USER/ADMIN role-based authorization
- Method-level security
- Resource ownership and IDOR protection
- Account lifecycle enforcement
- Immutable account ownership and account numbers
- Transactional deposits, withdrawals, and transfers
- Pessimistic locking for balance-changing operations
- Deterministic transfer lock ordering
- Separate `TRANSFER_OUT` and `TRANSFER_IN` ledger entries
- Paginated and filterable transaction history
- Centralized exception handling
- OpenAPI 3 documentation
- Interactive Swagger UI with JWT authorization
- 79 automated unit, integration, API, and security tests
- Multi-stage Docker image
- Docker Compose application + MySQL orchestration
- Isolated container networking
- Persistent MySQL Docker volume
- Environment-based secret configuration
- Spring Boot Actuator health monitoring
- Automated GitHub Actions CI
- Java 21 test execution on a fresh Linux CI runner

---

## Project Status

### Backend

- [x] REST API architecture
- [x] User registration
- [x] JWT authentication
- [x] Role-based authorization
- [x] Resource ownership authorization
- [x] IDOR protection
- [x] Account lifecycle management
- [x] Deposits and withdrawals
- [x] Account-to-account transfers
- [x] Transaction ledger
- [x] Pagination and filtering
- [x] Centralized exception handling

### Quality

- [x] DTO validation
- [x] Unit testing
- [x] Integration testing
- [x] Security testing
- [x] 79 automated tests
- [x] OpenAPI 3 documentation
- [x] Swagger UI

### DevOps

- [x] Externalized secrets
- [x] Docker image
- [x] Docker Compose
- [x] Containerized MySQL
- [x] Persistent Docker volume
- [x] Actuator health endpoint
- [x] GitHub Actions CI
- [x] Automated tests on push and pull request

---

## Roadmap

Potential future improvements include:

- Refresh-token support
- Email verification
- Password reset workflow
- Audit logging
- Rate limiting
- Flyway or Liquibase database migrations
- Additional observability and metrics
- Cloud deployment
- Frontend banking dashboard

---

## Repository

GitHub:

```text
https://github.com/Abhishek-Savita-3012/spring-boot-banking-application
```

---

## Author

**Abhishek Savita**

GitHub: `Abhishek-Savita-3012`

---

## Disclaimer

This project is intended for **learning, demonstration, and portfolio purposes**.

It is not a production banking platform and should not be used to process real financial transactions or store real banking credentials.