package com.example.survey.controller;

import com.example.survey.repository.UploadedImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Optional;
import java.util.regex.Pattern;

@RestController
@RequiredArgsConstructor
public class UploadedImageController {
    private static final Pattern SAFE_FILENAME = Pattern.compile("[a-zA-Z0-9_-]+\\.(?:jpg|jpeg|png|webp)");

    private final UploadedImageRepository uploadedImageRepository;

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    @GetMapping("/uploads/{filename:.+}")
    public ResponseEntity<byte[]> getImage(@PathVariable String filename) throws IOException {
        if (!SAFE_FILENAME.matcher(filename).matches()) {
            return ResponseEntity.notFound().build();
        }

        Optional<UploadedImageRepository.UploadedImage> stored = uploadedImageRepository.findByFilename(filename);
        if (stored.isPresent()) {
            UploadedImageRepository.UploadedImage image = stored.get();
            return imageResponse(image.content(), image.contentType());
        }

        // Images uploaded before database storage was introduced may still be on a persistent local volume.
        Path path = Paths.get(uploadDir).toAbsolutePath().normalize().resolve(filename);
        if (!Files.isRegularFile(path)) {
            return ResponseEntity.notFound().build();
        }
        String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        String contentType = switch (extension) {
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            default -> "image/jpeg";
        };
        return imageResponse(Files.readAllBytes(path), contentType);
    }

    private ResponseEntity<byte[]> imageResponse(byte[] content, String contentType) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
                .header("X-Content-Type-Options", "nosniff")
                .body(content);
    }
}
