CREATE TABLE IF NOT EXISTS menu_items (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255),
    category VARCHAR(255),
    price DOUBLE PRECISION,
    image_name VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS questions (
    id BIGSERIAL PRIMARY KEY,
    text VARCHAR(255) NOT NULL,
    question_type VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS options (
    id BIGSERIAL PRIMARY KEY,
    label VARCHAR(255) NOT NULL,
    icon VARCHAR(255),
    sub_description VARCHAR(255),
    question_id BIGINT NOT NULL REFERENCES questions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS answers (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    user_id VARCHAR(255),
    menu_item_id BIGINT REFERENCES menu_items(id) ON DELETE SET NULL,
    question_id BIGINT NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    option_id BIGINT REFERENCES options(id) ON DELETE SET NULL,
    response TEXT
);

CREATE INDEX IF NOT EXISTS idx_answers_menu_item_id ON answers(menu_item_id);
CREATE INDEX IF NOT EXISTS idx_answers_question_id ON answers(question_id);
CREATE INDEX IF NOT EXISTS idx_answers_option_id ON answers(option_id);
CREATE INDEX IF NOT EXISTS idx_answers_user_id ON answers(user_id);
