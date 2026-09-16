-- Ensure canonical options exist for evaluation questions (Mood and Weather)
-- This ensures evaluation questions have their dimensions in the database and are editable by admins.

INSERT INTO options (label, icon, sub_description, question_id)
SELECT opt.label, opt.icon, opt.sub_description, q.id
FROM questions q
CROSS JOIN (
    VALUES
        ('Energy', NULL, '(Wants something energizing)'),
        ('Comfort', NULL, '(Wants something warm or familiar)'),
        ('Refreshing', NULL, '(Wants something light or cooling)'),
        ('Healthy', NULL, '(Wants a healthier choice)'),
        ('Treat', NULL, '(Wants something enjoyable or indulgent)'),
        ('Focused', NULL, '(Wants to concentrate or study)'),
        ('Familiar', NULL, '(Wants a safe, familiar choice)'),
        ('Adventurous', NULL, '(Wants to try something new)'),
        ('Quick', NULL, '(Wants something convenient)')
) AS opt(label, icon, sub_description)
WHERE (LOWER(q.text) LIKE '%mood%' OR LOWER(q.text) LIKE '%emotion%')
  AND NOT EXISTS (
      SELECT 1 FROM options o WHERE o.question_id = q.id
  );

INSERT INTO options (label, icon, sub_description, question_id)
SELECT opt.label, opt.icon, opt.sub_description, q.id
FROM questions q
CROSS JOIN (
    VALUES
        ('Hot/Sunny', '☀️', NULL),
        ('Hot/Humid', '🌤️', NULL),
        ('Rainy', '🌧️', NULL),
        ('Cool Dry', '⛅', '(Note: Even in tropical climates, "cool dry" exists: breezy December–February days, air-conditioned spaces, or cool hill stations/evening breezes.)')
) AS opt(label, icon, sub_description)
WHERE LOWER(q.text) LIKE '%weather%'
  AND NOT EXISTS (
      SELECT 1 FROM options o WHERE o.question_id = q.id
  );
