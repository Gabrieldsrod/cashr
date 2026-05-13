ALTER TABLE transactions
    ADD COLUMN credit_card_id UUID REFERENCES credit_cards(id),
    ADD COLUMN invoice_date DATE;
