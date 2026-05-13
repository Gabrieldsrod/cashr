CREATE TYPE payment_method AS ENUM ('PIX', 'DEBIT_CARD', 'CREDIT_CARD', 'CASH', 'BANK_TRANSFER', 'BOLETO');

ALTER TABLE transactions ADD COLUMN payment_method payment_method;
