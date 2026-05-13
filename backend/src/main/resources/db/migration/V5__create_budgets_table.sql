CREATE TABLE budgets (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID            NOT NULL,
    category_id     UUID            NOT NULL,
    month           DATE            NOT NULL,
    limit_amount    NUMERIC(19,2)   NOT NULL CHECK (limit_amount > 0),

    CONSTRAINT fk_budgets_user     FOREIGN KEY (user_id)     REFERENCES users(id)      ON DELETE CASCADE,
    CONSTRAINT fk_budgets_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE,
    CONSTRAINT uq_budgets_user_category_month UNIQUE (user_id, category_id, month),
    CONSTRAINT chk_budgets_month_first_day CHECK (EXTRACT(DAY FROM month) = 1)
);

CREATE INDEX idx_budgets_user_id     ON budgets(user_id);
CREATE INDEX idx_budgets_category_id ON budgets(category_id);
CREATE INDEX idx_budgets_month       ON budgets(month);
