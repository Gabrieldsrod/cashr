CREATE TABLE accounts (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(255) NOT NULL,
    initial_balance NUMERIC(19, 2) NOT NULL,
    type            VARCHAR(20) NOT NULL CHECK (type IN ('CHECKING', 'SAVINGS', 'CASH', 'SALARY'))
);

CREATE TABLE categories (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(255) NOT NULL UNIQUE,
    description VARCHAR(255)
);

CREATE TABLE transactions (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type        VARCHAR(20) NOT NULL CHECK (type IN ('INCOME', 'EXPENSE')),
    amount      NUMERIC(19, 2) NOT NULL,
    date        TIMESTAMP NOT NULL DEFAULT now(),
    description VARCHAR(255),
    status      VARCHAR(20) NOT NULL CHECK (status IN ('PAID', 'PENDING')),
    category_id UUID REFERENCES categories(id) ON DELETE RESTRICT,
    account_id  UUID REFERENCES accounts(id) ON DELETE RESTRICT
);
