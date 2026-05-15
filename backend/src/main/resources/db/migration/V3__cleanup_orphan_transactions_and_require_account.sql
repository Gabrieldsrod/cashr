DELETE FROM transaction_tags
WHERE transaction_id IN (SELECT id FROM transactions WHERE account_id IS NULL);

DELETE FROM transactions
WHERE account_id IS NULL;

ALTER TABLE transactions
    ALTER COLUMN account_id SET NOT NULL;
