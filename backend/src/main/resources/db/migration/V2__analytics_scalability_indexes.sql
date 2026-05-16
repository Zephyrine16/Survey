CREATE INDEX IF NOT EXISTS idx_answers_menu_question
    ON answers(menu_item_id, question_id);

CREATE INDEX IF NOT EXISTS idx_answers_menu_user
    ON answers(menu_item_id, user_id);