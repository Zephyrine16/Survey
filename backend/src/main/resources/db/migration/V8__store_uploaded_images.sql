CREATE TABLE uploaded_images (
    filename VARCHAR(255) PRIMARY KEY,
    content_type VARCHAR(50) NOT NULL,
    content BYTEA NOT NULL,
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
