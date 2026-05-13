ALTER TABLE categories
    ADD COLUMN color VARCHAR(7),
    ADD CONSTRAINT chk_categories_color_format CHECK (color ~ '^#[0-9A-Fa-f]{6}$');
