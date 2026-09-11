-- Ensure the canonical evaluation questions exist in the questions table.
-- Using WHERE NOT EXISTS makes this migration idempotent across fresh setups and existing databases.

INSERT INTO questions (text, question_type)
SELECT 'Question 1 — Mood Association: How suitable is this item for each of the following moods?', 'TEXT'
WHERE NOT EXISTS (
    SELECT 1 FROM questions WHERE LOWER(text) LIKE '%mood%' OR LOWER(text) LIKE '%emotion%'
);

INSERT INTO questions (text, question_type)
SELECT 'Question 2 — Weather Association: How suitable is this item for each of the following weather conditions?', 'TEXT'
WHERE NOT EXISTS (
    SELECT 1 FROM questions WHERE LOWER(text) LIKE '%weather%'
);
