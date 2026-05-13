CREATE TYPE recurrence_frequency AS ENUM ('DAILY', 'WEEKLY', 'MONTHLY', 'YEARLY');

CREATE TABLE recurrences (
    id               UUID                 PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID                 NOT NULL,
    account_id       UUID                 NOT NULL,
    category_id      UUID,
    description      VARCHAR(255)         NOT NULL,
    amount           NUMERIC(19,2)        NOT NULL CHECK (amount > 0),
    type             VARCHAR(10)          NOT NULL CHECK (type IN ('INCOME', 'EXPENSE')),
    frequency        recurrence_frequency NOT NULL,
    day_of_month     SMALLINT             CHECK (day_of_month BETWEEN 1 AND 31),
    next_occurrence  DATE                 NOT NULL,
    is_active        BOOLEAN              NOT NULL DEFAULT TRUE,

    CONSTRAINT fk_recurrences_user     FOREIGN KEY (user_id)     REFERENCES users(id)      ON DELETE CASCADE,
    CONSTRAINT fk_recurrences_account  FOREIGN KEY (account_id)  REFERENCES accounts(id)   ON DELETE CASCADE,
    CONSTRAINT fk_recurrences_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL
);

CREATE INDEX idx_recurrences_user_id         ON recurrences(user_id);
CREATE INDEX idx_recurrences_next_occurrence ON recurrences(next_occurrence) WHERE is_active = TRUE;
