# CashR API Contract

Base URL: `/api`

OpenAPI / Swagger UI: `/swagger-ui.html` &nbsp;·&nbsp; OpenAPI JSON: `/v3/api-docs`

---

## Authentication

All endpoints **except** `/api/auth/**` require a JWT in the `Authorization` header:

```
Authorization: Bearer <access_token>
```

The authenticated user is resolved server-side from the token. **Never** send `userId` in a request body or query param — it is ignored and the JWT principal is used instead.

### POST `/api/auth/register`
Register a new user and receive a JWT.

**Request Body:**
```json
{
  "fullName": "string (required, non-blank)",
  "email": "string (required, valid email)",
  "password": "string (required, min 8 chars)"
}
```

**Response `201`:**
```json
{
  "access_token": "string (JWT)",
  "token_type": "Bearer",
  "user_id": "uuid",
  "email": "string"
}
```

---

### POST `/api/auth/login`
Authenticate and receive a JWT.

**Request Body:**
```json
{
  "email": "string (required, valid email)",
  "password": "string (required)"
}
```

**Response `200`:** same shape as register response.

---

## Enums

| Enum | Values |
|------|--------|
| `TransactionType` | `INCOME`, `EXPENSE` |
| `TransactionStatus` | `PENDING`, `PAID` |
| `PaymentMethod` | `PIX`, `DEBIT_CARD`, `CREDIT_CARD`, `CASH`, `BANK_TRANSFER`, `BOLETO` |
| `Currency` | `BRL`, `USD`, `EUR` |
| `AccountType` | `CHECKING`, `SAVINGS`, `CASH`, `SALARY`, `DIGITAL_WALLET` |
| `BudgetStatus` | `OK`, `ATENCAO`, `ESTOURADO` |

---

## Accounts `/api/accounts`

### POST `/api/accounts`
Create a new account.

**Request Body:**
```json
{
  "name": "string (required, non-blank)",
  "type": "AccountType (required)",
  "currency": "Currency (required)",
  "initialBalance": "decimal >= 0 (required)"
}
```

**Response `201`:**
```json
{
  "id": "uuid",
  "userId": "uuid",
  "name": "string",
  "type": "AccountType",
  "currency": "Currency",
  "initialBalance": "decimal",
  "currentBalance": "decimal"
}
```

> `currentBalance` is computed on read as `initialBalance + Σ(INCOME PAID) − Σ(EXPENSE PAID)`. Only `PAID` transactions count.

---

### PUT `/api/accounts/{id}`
Update an account.

**Path Params:** `id` (UUID)

**Request Body:** same shape as POST.

> **Only `name` and `type` are persisted.** `initialBalance` is immutable after creation (business invariant) and `currency` is currently ignored on update.

**Response `200`:** account object.

---

### GET `/api/accounts`
List all accounts owned by the authenticated user.

**Response `200`:** array of account objects.

---

### GET `/api/accounts/{id}`
Get an account by ID. Returns `404` if the account does not belong to the authenticated user.

**Path Params:** `id` (UUID)

**Query Params (optional):**
- `start` (LocalDate)
- `end` (LocalDate)

> When `start` and `end` are provided, `currentBalance` is recomputed using only PAID transactions whose `competenceDate` falls inside `[start, end]`. Useful for "balance as of period X".

**Response `200`:** account object.

---

### GET `/api/accounts/{id}/statement`
Account statement for a date range, with running balance per line.

**Path Params:** `id` (UUID)

**Query Params:**
- `startDate` (LocalDate, required)
- `endDate` (LocalDate, required)
- `status` (TransactionStatus, optional — filter PAID or PENDING)

**Response `200`:**
```json
[
  {
    "id": "uuid",
    "type": "TransactionType",
    "amount": "decimal",
    "competenceDate": "date",
    "description": "string",
    "status": "TransactionStatus",
    "categoryId": "uuid",
    "categoryName": "string",
    "runningBalance": "decimal"
  }
]
```

---

### DELETE `/api/accounts/{id}`
Delete an account. Fails with `422` if there are transactions referencing it (ON DELETE RESTRICT).

**Response `204`:** no content.

---

## Categories `/api/categories`

### POST `/api/categories`
Create a new category.

**Request Body:**
```json
{
  "name": "string (required, non-blank)",
  "description": "string (optional)",
  "color": "string matching ^#[0-9A-Fa-f]{6}$ (optional)"
}
```

**Response `201`:**
```json
{
  "id": "uuid",
  "userId": "uuid",
  "name": "string",
  "description": "string",
  "color": "string"
}
```

---

### PUT `/api/categories/{id}`
Update a category. **Request Body:** same as POST.

**Response `200`:** category object.

---

### GET `/api/categories`
List all categories of the authenticated user.

**Response `200`:** array of category objects.

---

### GET `/api/categories/{id}`
Get a category by ID.

**Response `200`:** category object.

---

### DELETE `/api/categories/{id}`
Delete a category. Fails with `422` if any transaction references it.

**Response `204`:** no content.

---

## Credit Cards `/api/credit-cards`

### POST `/api/credit-cards`
Create a new credit card.

**Request Body:**
```json
{
  "name": "string (required, non-blank)",
  "bank": "string (required, non-blank)",
  "accountId": "uuid (required, account to debit on invoice payment)",
  "creditLimit": "decimal > 0 (required)",
  "closingDay": "integer 1-31 (required)",
  "dueDay": "integer 1-31 (required)"
}
```

**Response `201`:**
```json
{
  "id": "uuid",
  "userId": "uuid",
  "accountId": "uuid",
  "name": "string",
  "bank": "string",
  "creditLimit": "decimal",
  "closingDay": "integer",
  "dueDay": "integer"
}
```

---

### PUT `/api/credit-cards/{id}`
Update a credit card. **Request Body:** same as POST.

**Response `200`:** credit card object.

---

### GET `/api/credit-cards`
List all credit cards of the authenticated user.

**Response `200`:** array of credit card objects.

---

### GET `/api/credit-cards/{id}`
Get a credit card by ID.

**Response `200`:** credit card object.

---

### GET `/api/credit-cards/{id}/invoice`
Get the invoice for a specific month.

**Path Params:** `id` (UUID)

**Query Params:**
- `year` (integer 2000-2100, required)
- `month` (integer 1-12, required)

**Response `200`:**
```json
{
  "creditCard": { "...CreditCardResponse" },
  "invoiceDate": "date",
  "totalAmount": "decimal",
  "transactions": [ { "...TransactionResponse" } ]
}
```

---

### DELETE `/api/credit-cards/{id}`
Delete a credit card.

**Response `204`:** no content.

---

## Transactions `/api/transactions`

### GET `/api/transactions`
List transactions (paginated) for the authenticated user.

**Query Params:**
- `type` (TransactionType, optional)
- `status` (TransactionStatus, optional)
- `year` (integer 2000-2100, optional — required together with `month` for month filter)
- `month` (integer 1-12, optional)
- `page` (integer, default `0`)
- `size` (integer, default `20`)
- `sort` (string, default `competenceDate,desc`)

**Response `200`:** Spring `Page` wrapper.
```json
{
  "content": [ { "...TransactionResponse" } ],
  "totalElements": "long",
  "totalPages": "integer",
  "number": "integer",
  "size": "integer"
}
```

#### `TransactionResponse` shape
```json
{
  "id": "uuid",
  "userId": "uuid",
  "accountId": "uuid",
  "accountName": "string",
  "type": "TransactionType",
  "status": "TransactionStatus",
  "currency": "Currency",
  "amount": "decimal",
  "competenceDate": "date",
  "createdAt": "datetime",
  "description": "string",
  "category": { "...CategoryResponse or null" },
  "paymentMethod": "PaymentMethod or null",
  "creditCard": { "...CreditCardResponse or null" },
  "invoiceDate": "date or null",
  "installmentGroupId": "uuid or null",
  "installmentNumber": "integer or null",
  "totalInstallments": "integer or null",
  "tags": [ { "...TagResponse" } ]
}
```

---

### GET `/api/transactions/{id}`
Get a transaction by ID. Returns `404` if it does not belong to the authenticated user.

**Response `200`:** `TransactionResponse`.

---

### GET `/api/transactions/installment-group/{groupId}`
List all parcels in an installment group (ordered by `installmentNumber`).

**Response `200`:** array of `TransactionResponse`. Empty array if the group does not belong to the authenticated user.

---

### POST `/api/transactions`
Create a transaction.

**Request Body:**
```json
{
  "type": "TransactionType (required)",
  "status": "TransactionStatus (required)",
  "currency": "Currency (required)",
  "amount": "decimal > 0 (required)",
  "competenceDate": "date (required)",
  "accountId": "uuid (required)",
  "categoryId": "uuid (required)",
  "description": "string (optional)",
  "paymentMethod": "PaymentMethod (optional)",
  "creditCardId": "uuid (required iff paymentMethod = CREDIT_CARD)"
}
```

**Response `201`:** `TransactionResponse`.

---

### PUT `/api/transactions/{id}`
Update a single transaction. Affects **only** this row, even if it belongs to an installment group — use the group endpoint to bulk-edit.

**Path Params:** `id` (UUID)

**Request Body:** same as POST.

> `paymentMethod` is **immutable** on update — sending a different value than the existing one returns `422`. If you got it wrong, delete and recreate.

**Response `200`:** `TransactionResponse`.

---

### DELETE `/api/transactions/{id}`
Delete a transaction.

**Response `204`:** no content.

---

### POST `/api/transactions/installments`
Create N parcels in one go. The total `amount` is divided evenly across `totalInstallments` parcels (rounded `HALF_UP`). Each parcel gets `competenceDate + i months` and shares an `installmentGroupId`.

**Request Body:**
```json
{
  "type": "TransactionType (required)",
  "status": "TransactionStatus (required)",
  "currency": "Currency (required)",
  "amount": "decimal > 0 (required, total to split)",
  "competenceDate": "date (required, first parcel)",
  "accountId": "uuid (required)",
  "categoryId": "uuid (required)",
  "totalInstallments": "integer >= 2 (required)",
  "description": "string (optional)",
  "paymentMethod": "PaymentMethod (optional)",
  "creditCardId": "uuid (required iff paymentMethod = CREDIT_CARD)"
}
```

**Response `201`:** array of `TransactionResponse` (one per parcel).

---

### PUT `/api/transactions/installment-group/{groupId}`
Bulk-update every parcel in a group. Each parcel preserves its own `competenceDate` (and the per-parcel `invoiceDate` is recomputed if the credit card changes).

**Path Params:** `groupId` (UUID)

**Request Body:**
```json
{
  "type": "TransactionType (required)",
  "status": "TransactionStatus (required)",
  "currency": "Currency (required)",
  "amount": "decimal > 0 (required, per-parcel amount)",
  "accountId": "uuid (required)",
  "categoryId": "uuid (required)",
  "description": "string (optional)",
  "paymentMethod": "PaymentMethod (optional)",
  "creditCardId": "uuid (required iff paymentMethod = CREDIT_CARD)"
}
```

> Same `paymentMethod` immutability rule as the single update: a mismatch against the group's existing `paymentMethod` returns `422`.

**Response `200`:** array of `TransactionResponse`.

---

### GET `/api/transactions/balance`
Net flow of the given month considering only `PAID` transactions.

**Query Params:**
- `year` (integer 2000-2100, required)
- `month` (integer 1-12, required)

**Response `200`:** `decimal` — `Σ(INCOME PAID) − Σ(EXPENSE PAID)`.

---

## Tags `/api/tags`

### POST `/api/tags`
Create a tag.

**Request Body:**
```json
{
  "name": "string (required, non-blank)"
}
```

**Response `201`:**
```json
{
  "id": "uuid",
  "userId": "uuid",
  "name": "string"
}
```

---

### GET `/api/tags`
List all tags of the authenticated user.

**Response `200`:** array of tag objects.

---

### DELETE `/api/tags/{id}`
Delete a tag.

**Response `204`:** no content.

---

### POST `/api/tags/transaction/{transactionId}`
Attach one or more tag names to a transaction. Tags that do not exist are created.

**Path Params:** `transactionId` (UUID)

**Request Body:**
```json
["string", "string", ...]
```

**Response `204`:** no content.

---

### DELETE `/api/tags/transaction/{transactionId}/{tagId}`
Detach a tag from a transaction.

**Response `204`:** no content.

---

## Budgets `/api/budgets`

A budget is the monthly spending limit for one category. Uniqueness is enforced on `(userId, categoryId, month)` — month is always stored as the first day of the month.

### POST `/api/budgets`
Create a budget for a category/month.

**Request Body:**
```json
{
  "categoryId": "uuid (required)",
  "year": "integer 2000-2100 (required)",
  "month": "integer 1-12 (required)",
  "limitAmount": "decimal > 0 (required)"
}
```

**Response `201`:**
```json
{
  "id": "uuid",
  "categoryId": "uuid",
  "categoryName": "string",
  "month": "date (always day = 01)",
  "limitAmount": "decimal"
}
```

> Returns `422` if a budget already exists for this `(categoryId, year, month)` tuple, or `404` if the category does not belong to the authenticated user.

---

### PUT `/api/budgets/{id}`
Update a budget. **Request Body:** same shape as POST.

**Response `200`:** budget object.

> If `categoryId` or `(year, month)` change, the uniqueness rule is re-checked and a conflict returns `422`.

---

### GET `/api/budgets`
List all budgets of the authenticated user, optionally filtered by month.

**Query Params (optional, must be sent together):**
- `year` (integer 2000-2100)
- `month` (integer 1-12)

**Response `200`:** array of budget objects (sorted by `month DESC` when unfiltered, by `category.name ASC` when filtered).

---

### GET `/api/budgets/{id}`
Get a budget by ID.

**Response `200`:** budget object.

---

### GET `/api/budgets/{id}/status`
Compute the budget status for its month. `spent` is the sum of `EXPENSE PAID` transactions on the same category whose `competenceDate` falls inside the budget's month.

**Response `200`:**
```json
{
  "budgetId": "uuid",
  "categoryId": "uuid",
  "categoryName": "string",
  "month": "date",
  "limitAmount": "decimal",
  "spent": "decimal",
  "percentageUsed": "decimal (0-100+, 2 decimal places)",
  "status": "BudgetStatus"
}
```

> Status rule: `percentageUsed >= 100` → `ESTOURADO`; `>= 80` → `ATENCAO`; otherwise `OK`.

---

### DELETE `/api/budgets/{id}`
Delete a budget.

**Response `204`:** no content.

---

## Error Responses

All errors share the same JSON shape (emitted by `GlobalExceptionHandler`):

```json
{
  "timestamp": "2026-05-15T10:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Transação não encontrada",
  "fieldErrors": null
}
```

When validation fails, `fieldErrors` is a `{ "fieldName": "message" }` map and `message` is `"Validation failed"`.

| Status | Trigger |
|--------|---------|
| `400` | `MethodArgumentNotValidException` (Bean Validation), `IllegalArgumentException` |
| `401` | Missing or invalid JWT, or bad credentials on `/auth/login` |
| `403` | Authenticated but lacking authorization |
| `404` | `ResourceNotFoundException` — resource doesn't exist or doesn't belong to the caller |
| `422` | `BusinessException` — domain rule violation (e.g. delete account with transactions, change `paymentMethod` on update, deleted account/credit card not found at write time) |
| `500` | Uncaught exception — message is generic, no stack trace leaked |
