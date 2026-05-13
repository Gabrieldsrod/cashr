CREATE TYPE currency AS ENUM ('BRL', 'USD', 'EUR');

ALTER TABLE accounts
    ADD COLUMN currency currency NOT NULL DEFAULT 'BRL';
