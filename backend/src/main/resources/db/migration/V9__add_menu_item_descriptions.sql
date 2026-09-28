ALTER TABLE menu_items
    ADD COLUMN IF NOT EXISTS description TEXT,
    ADD COLUMN IF NOT EXISTS description_updated_at TIMESTAMP;
