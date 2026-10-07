package com.example.survey.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class UploadedImageRepository {
    private final JdbcTemplate jdbcTemplate;

    public UploadedImageRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(String filename, String contentType, byte[] content) {
        jdbcTemplate.update("""
                INSERT INTO uploaded_images (filename, content_type, content)
                VALUES (?, ?, ?)
                ON CONFLICT (filename) DO UPDATE
                SET content_type = EXCLUDED.content_type, content = EXCLUDED.content
                """,
                filename, contentType, content);
    }

    public Optional<UploadedImage> findByFilename(String filename) {
        List<UploadedImage> images = jdbcTemplate.query(
                "SELECT content_type, content FROM uploaded_images WHERE filename = ?",
                (rs, rowNum) -> new UploadedImage(rs.getString("content_type"), rs.getBytes("content")),
                filename);
        return images.stream().findFirst();
    }

    public record UploadedImage(String contentType, byte[] content) {}
}
