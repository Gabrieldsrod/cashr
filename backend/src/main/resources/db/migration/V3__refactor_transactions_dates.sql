ALTER TABLE transactions
    RENAME COLUMN date TO created_at;

ALTER TABLE transactions
    ALTER COLUMN created_at SET DEFAULT now(),
    ALTER COLUMN created_at TYPE TIMESTAMP USING created_at::TIMESTAMP;

ALTER TABLE transactions
    ADD COLUMN competence_date DATE NOT NULL DEFAULT now();

CREATE INDEX idx_transactions_competence_date ON transactions(competence_date);
