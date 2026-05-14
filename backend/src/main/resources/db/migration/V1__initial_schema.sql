CREATE TYPE currency AS ENUM ('BRL', 'USD', 'EUR');
CREATE TYPE payment_method AS ENUM ('PIX', 'DEBIT_CARD', 'CREDIT_CARD', 'CASH', 'BANK_TRANSFER', 'BOLETO');
CREATE TYPE recurrence_frequency AS ENUM ('DAILY', 'WEEKLY', 'MONTHLY', 'YEARLY');

CREATE TABLE users (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name     VARCHAR(255) NOT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_email ON users(email);

CREATE TABLE accounts (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(255) NOT NULL,
    initial_balance NUMERIC(19,2) NOT NULL,
    type            VARCHAR(20)  NOT NULL CHECK (type IN ('CHECKING', 'SAVINGS', 'CASH', 'SALARY')),
    currency        currency     NOT NULL DEFAULT 'BRL',
    user_id         UUID         NOT NULL,
    CONSTRAINT fk_accounts_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_accounts_user_id ON accounts(user_id);

CREATE TABLE categories (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    color       VARCHAR(7)   CHECK (color ~ '^#[0-9A-Fa-f]{6}$'),
    user_id     UUID         NOT NULL,
    CONSTRAINT fk_categories_user      FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uq_categories_name_user UNIQUE (name, user_id)
);

CREATE INDEX idx_categories_user_id ON categories(user_id);

CREATE TABLE credit_cards (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name         VARCHAR(255) NOT NULL,
    bank         VARCHAR(255) NOT NULL,
    closing_day  INT          NOT NULL CHECK (closing_day BETWEEN 1 AND 31),
    due_day      INT          NOT NULL CHECK (due_day BETWEEN 1 AND 31),
    credit_limit NUMERIC(19,2) NOT NULL,
    user_id      UUID         NOT NULL,
    account_id   UUID         NOT NULL,
    CONSTRAINT fk_credit_cards_user    FOREIGN KEY (user_id)    REFERENCES users(id)    ON DELETE CASCADE,
    CONSTRAINT fk_credit_cards_account FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE RESTRICT
);

CREATE TABLE transactions (
    id                   UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    type                 VARCHAR(20)   NOT NULL CHECK (type IN ('INCOME', 'EXPENSE')),
    amount               NUMERIC(19,2) NOT NULL,
    created_at           TIMESTAMP     NOT NULL DEFAULT now(),
    competence_date      DATE          NOT NULL DEFAULT now(),
    description          VARCHAR(255),
    status               VARCHAR(20)   NOT NULL CHECK (status IN ('PAID', 'PENDING')),
    currency             currency      NOT NULL DEFAULT 'BRL',
    payment_method       payment_method,
    credit_card_id       UUID,
    invoice_date         DATE,
    installment_group_id UUID,
    installment_number   INT,
    total_installments   INT,
    category_id          UUID,
    account_id           UUID,
    user_id              UUID          NOT NULL,
    CONSTRAINT fk_transactions_category    FOREIGN KEY (category_id)    REFERENCES categories(id)   ON DELETE RESTRICT,
    CONSTRAINT fk_transactions_account     FOREIGN KEY (account_id)     REFERENCES accounts(id)     ON DELETE RESTRICT,
    CONSTRAINT fk_transactions_credit_card FOREIGN KEY (credit_card_id) REFERENCES credit_cards(id) ON DELETE SET NULL,
    CONSTRAINT fk_transactions_user        FOREIGN KEY (user_id)        REFERENCES users(id)        ON DELETE CASCADE
);

CREATE INDEX idx_transactions_user_id        ON transactions(user_id);
CREATE INDEX idx_transactions_competence_date ON transactions(competence_date);

CREATE TABLE budgets (
    id           UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    month        DATE          NOT NULL,
    limit_amount NUMERIC(19,2) NOT NULL CHECK (limit_amount > 0),
    user_id      UUID          NOT NULL,
    category_id  UUID          NOT NULL,
    CONSTRAINT fk_budgets_user                  FOREIGN KEY (user_id)     REFERENCES users(id)       ON DELETE CASCADE,
    CONSTRAINT fk_budgets_category              FOREIGN KEY (category_id) REFERENCES categories(id)  ON DELETE CASCADE,
    CONSTRAINT uq_budgets_user_category_month   UNIQUE (user_id, category_id, month),
    CONSTRAINT chk_budgets_month_first_day      CHECK (EXTRACT(DAY FROM month) = 1)
);

CREATE INDEX idx_budgets_user_id     ON budgets(user_id);
CREATE INDEX idx_budgets_category_id ON budgets(category_id);
CREATE INDEX idx_budgets_month       ON budgets(month);

CREATE TABLE recurrences (
    id              UUID                 PRIMARY KEY DEFAULT gen_random_uuid(),
    description     VARCHAR(255)         NOT NULL,
    amount          NUMERIC(19,2)        NOT NULL CHECK (amount > 0),
    type            VARCHAR(10)          NOT NULL CHECK (type IN ('INCOME', 'EXPENSE')),
    frequency       recurrence_frequency NOT NULL,
    day_of_month    INTEGER              CHECK (day_of_month BETWEEN 1 AND 31),
    next_occurrence DATE                 NOT NULL,
    is_active       BOOLEAN              NOT NULL DEFAULT TRUE,
    user_id         UUID                 NOT NULL,
    account_id      UUID                 NOT NULL,
    category_id     UUID,
    CONSTRAINT fk_recurrences_user     FOREIGN KEY (user_id)     REFERENCES users(id)      ON DELETE CASCADE,
    CONSTRAINT fk_recurrences_account  FOREIGN KEY (account_id)  REFERENCES accounts(id)   ON DELETE CASCADE,
    CONSTRAINT fk_recurrences_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL
);

CREATE INDEX idx_recurrences_user_id         ON recurrences(user_id);
CREATE INDEX idx_recurrences_next_occurrence ON recurrences(next_occurrence) WHERE is_active = TRUE;

CREATE TABLE tags (
    id      UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name    VARCHAR(50) NOT NULL,
    user_id UUID        NOT NULL,
    CONSTRAINT fk_tags_user    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uq_tags_user_name UNIQUE (user_id, name)
);

CREATE INDEX idx_tags_user_id_name ON tags(user_id, name);

CREATE TABLE transaction_tags (
    transaction_id UUID NOT NULL,
    tag_id         UUID NOT NULL,
    CONSTRAINT pk_transaction_tags     PRIMARY KEY (transaction_id, tag_id),
    CONSTRAINT fk_transaction_tags_tx  FOREIGN KEY (transaction_id) REFERENCES transactions(id) ON DELETE CASCADE,
    CONSTRAINT fk_transaction_tags_tag FOREIGN KEY (tag_id)         REFERENCES tags(id)         ON DELETE CASCADE
);

CREATE INDEX idx_transaction_tags_tag_id ON transaction_tags(tag_id);
