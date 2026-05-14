# CashR API Contract

Base URL: `/api`

---

## Enums

| Enum | Values |
|------|--------|
| `TransactionType` | `INCOME`, `EXPENSE` |
| `TransactionStatus` | `PENDING`, `PAID` |
| `PaymentMethod` | `PIX`, `DEBIT_CARD`, `CREDIT_CARD`, `CASH`, `BANK_TRANSFER`, `BOLETO` |
| `Currency` | `BRL`, `USD`, `EUR` |
| `AccountType` | `CHECKING`, `SAVINGS`, `CASH`, `SALARY` |
| `BudgetStatus` | `OK`, `ATENCAO`, `ESTOURADO` |

---

## Accounts `/api/accounts`

### POST `/api/accounts`
Create a new account.

**Request Body:**
```json
{
  "userId": "uuid (required)",
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

---

### PUT `/api/accounts/{id}`
Update an account.

**Path Params:** `id` (UUID)

**Request Body:** same as POST

**Response `200`:** same as POST response

---

### GET `/api/accounts`
List all accounts for a user.

**Query Params:** `userId` (UUID, required)

**Response `200`:** array of account objects (see POST response)

---

### GET `/api/accounts/{id}`
Get account by ID.

**Path Params:** `id` (UUID)

**Query Params:**
- `start` (LocalDate, optional)
- `end` (LocalDate, optional)

**Response `200`:** single account object

---

### GET `/api/accounts/{id}/statement`
Get account statement for a date range.

**Path Params:** `id` (UUID)

**Query Params:**
- `startDate` (LocalDate, required)
- `endDate` (LocalDate, required)
- `status` (TransactionStatus, optional)

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
Delete an account.

**Path Params:** `id` (UUID)

**Response `204`:** no content

---

## Categories `/api/categories`

### POST `/api/categories`
Create a new category.

**Request Body:**
```json
{
  "userId": "uuid (required)",
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
Update a category.

**Path Params:** `id` (UUID)

**Request Body:** same as POST

**Response `200`:** same as POST response

---

### GET `/api/categories`
List all categories for a user.

**Query Params:** `userId` (UUID, required)

**Response `200`:** array of category objects

---

### GET `/api/categories/{id}`
Get category by ID.

**Path Params:** `id` (UUID)

**Response `200`:** single category object

---

### DELETE `/api/categories/{id}`
Delete a category.

**Path Params:** `id` (UUID)

**Response `204`:** no content

---

## Credit Cards `/api/credit-cards`

### POST `/api/credit-cards`
Create a new credit card.

**Request Body:**
```json
{
  "userId": "uuid (required)",
  "accountId": "uuid (required)",
  "name": "string (required, non-blank)",
  "bank": "string (required, non-blank)",
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
Update a credit card.

**Path Params:** `id` (UUID)

**Request Body:** same as POST

**Response `200`:** same as POST response

---

### GET `/api/credit-cards`
List all credit cards for a user.

**Query Params:** `userId` (UUID, required)

**Response `200`:** array of credit card objects

---

### GET `/api/credit-cards/{id}`
Get credit card by ID.

**Path Params:** `id` (UUID)

**Response `200`:** single credit card object

---

### GET `/api/credit-cards/{id}/invoice`
Get invoice for a specific month.

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

**Path Params:** `id` (UUID)

**Response `204`:** no content

---

## Transactions `/api/transactions`

### GET `/api/transactions`
List transactions (paginated).

**Query Params:**
- `userId` (UUID, required)
- `type` (TransactionType, optional)
- `status` (TransactionStatus, optional)
- `year` (integer, optional)
- `month` (integer 1-12, optional)
- `page` (integer, default: 0)
- `size` (integer, default: 20)
- `sort` (string, default: `competenceDate,desc`)

**Response `200`:**
```json
{
  "content": [
    {
      "id": "uuid",
      "userId": "uuid",
      "type": "TransactionType",
      "status": "TransactionStatus",
      "currency": "Currency",
      "amount": "decimal",
      "competenceDate": "date",
      "createdAt": "datetime",
      "description": "string",
      "category": { "...CategoryResponse" },
      "paymentMethod": "PaymentMethod",
      "creditCard": { "...CreditCardResponse or null" },
      "invoiceDate": "date or null",
      "installmentGroupId": "uuid or null",
      "installmentNumber": "integer or null",
      "totalInstallments": "integer or null"
    }
  ],
  "totalElements": "long",
  "totalPages": "integer",
  "number": "integer",
  "size": "integer"
}
```

---

### GET `/api/transactions/{id}`
Get transaction by ID.

**Path Params:** `id` (UUID)

**Response `200`:** single transaction object (see content item above)

---

### GET `/api/transactions/installment-group/{groupId}`
Get all transactions in an installment group.

**Path Params:** `groupId` (UUID)

**Response `200`:** array of transaction objects

---

### POST `/api/transactions`
Create a transaction.

**Request Body:**
```json
{
  "userId": "uuid (required)",
  "type": "TransactionType (required)",
  "status": "TransactionStatus (required)",
  "currency": "Currency (required)",
  "amount": "decimal > 0 (required)",
  "competenceDate": "date (required)",
  "categoryId": "uuid (required)",
  "description": "string (optional)",
  "paymentMethod": "PaymentMethod (optional)",
  "creditCardId": "uuid (optional)"
}
```

**Response `201`:** single transaction object

---

### POST `/api/transactions/installments`
Create installment transactions (splits total amount evenly).

**Request Body:**
```json
{
  "userId": "uuid (required)",
  "type": "TransactionType (required)",
  "status": "TransactionStatus (required)",
  "currency": "Currency (required)",
  "amount": "decimal > 0 (required, total amount to split)",
  "competenceDate": "date (required, first installment date)",
  "categoryId": "uuid (required)",
  "totalInstallments": "integer >= 2 (required)",
  "description": "string (optional)",
  "paymentMethod": "PaymentMethod (optional)",
  "creditCardId": "uuid (optional)"
}
```

**Response `201`:** array of transaction objects (one per installment)

---

### GET `/api/transactions/balance`
Get net balance for a given month (PAID transactions only).

**Query Params:**
- `userId` (UUID, required)
- `year` (integer 2000-2100, required)
- `month` (integer 1-12, required)

**Response `200`:** `decimal` — monthly net flow (income − expenses)

---

## Error Responses

| Status | Meaning |
|--------|---------|
| `400` | Validation error (invalid or missing fields) |
| `404` | Resource not found |
| `409` | Business rule violation (`BusinessException`) |
| `500` | Unexpected server error |
