# Cashr API — Backend

REST API for personal finance management. Tracks income, expenses, accounts, credit cards, budgets, and recurring transactions.

## Tech Stack

- **Java 21** / Spring Boot 3.5
- **Spring Data JPA** / Hibernate
- **PostgreSQL**
- **Flyway** (database migrations)
- **Maven**
- **Lombok**
- **SpringDoc OpenAPI** (Swagger UI)

## Prerequisites

- Java 21+
- Maven 3.8+
- PostgreSQL running on `localhost:5432`
- Database `cashr` created

## Setup

1. **Create the database:**
   ```sql
   CREATE DATABASE cashr;
   ```

2. **Configure credentials** in `src/main/resources/application.properties`:
   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/cashr
   spring.datasource.username=postgres
   spring.datasource.password=admin
   ```

3. **Run the application** (Flyway will apply migrations automatically):
   ```bash
   ./mvnw spring-boot:run
   ```

The server starts on `http://localhost:8080`.

## API Documentation

Swagger UI is available at `http://localhost:8080/swagger-ui.html` after the application starts.

## Endpoints

### Accounts — `/api/accounts`

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/accounts` | Create account |
| `GET` | `/api/accounts` | List all accounts |
| `GET` | `/api/accounts/{id}` | Get account by ID |
| `PUT` | `/api/accounts/{id}` | Update account |
| `DELETE` | `/api/accounts/{id}` | Delete account |
| `GET` | `/api/accounts/{id}/statement` | Account statement (params: `startDate`, `endDate`, `status`) |

### Transactions — `/api/transactions`

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/transactions` | List transactions (filters: `type`, `status`, `year`, `month`) |
| `GET` | `/api/transactions/{id}` | Get transaction by ID |
| `POST` | `/api/transactions` | Create transaction |
| `POST` | `/api/transactions/installments` | Create installment transactions |
| `GET` | `/api/transactions/installment-group/{groupId}` | Get installment group |
| `GET` | `/api/transactions/balance` | Monthly balance (params: `year`, `month`) |

### Categories — `/api/categories`

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/categories` | Create category |
| `GET` | `/api/categories` | List all categories |
| `GET` | `/api/categories/{id}` | Get category by ID |
| `PUT` | `/api/categories/{id}` | Update category |
| `DELETE` | `/api/categories/{id}` | Delete category |

### Credit Cards — `/api/credit-cards`

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/credit-cards` | Create credit card |
| `GET` | `/api/credit-cards` | List all credit cards |
| `GET` | `/api/credit-cards/{id}` | Get credit card by ID |
| `PUT` | `/api/credit-cards/{id}` | Update credit card |
| `DELETE` | `/api/credit-cards/{id}` | Delete credit card |
| `GET` | `/api/credit-cards/{id}/invoice` | Get invoice (params: `year`, `month`) |

## Project Structure

```
src/main/java/com/gabrieldsrod/cashr/api/
├── controller/       # REST controllers
├── service/          # Business logic
├── repository/       # JPA repositories
├── model/            # JPA entities and enums
├── dto/              # Request/Response DTOs
└── exception/        # Global exception handling

src/main/resources/
├── application.properties
└── db/migration/     # Flyway SQL migrations (V0–V13)
```

## Useful Commands

```bash
# Compile and check for errors
./mvnw clean compile

# Run tests
./mvnw test

# Package
./mvnw clean package -DskipTests
```

## Database Migrations

Flyway runs migrations automatically on startup. Migration files are located in `src/main/resources/db/migration/`.

To inspect applied migrations, connect to the database and run:
```sql
SELECT * FROM flyway_schema_history ORDER BY installed_rank;
```
