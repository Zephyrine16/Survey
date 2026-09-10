-- Ensure the two Section 1 demographic questions always exist in the questions table.
-- Using WHERE NOT EXISTS makes this migration idempotent even without a UNIQUE constraint.

INSERT INTO questions (text, question_type)
SELECT 'Age Group', 'RADIO'
WHERE NOT EXISTS (SELECT 1 FROM questions WHERE text = 'Age Group');

INSERT INTO questions (text, question_type)
SELECT 'How often do you dine at cafés or restaurants?', 'RADIO'
WHERE NOT EXISTS (SELECT 1 FROM questions WHERE text = 'How often do you dine at cafés or restaurants?');

