ALTER TABLE transactions
    ADD COLUMN installment_group_id UUID,
    ADD COLUMN installment_number   INT,
    ADD COLUMN total_installments   INT;
