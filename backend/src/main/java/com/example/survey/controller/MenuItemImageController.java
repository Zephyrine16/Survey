package com.example.survey.controller;

import com.example.survey.repository.MenuItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/menu-items")
@RequiredArgsConstructor
public class MenuItemImageController {

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    private final MenuItemRepository menuItemRepository;

    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp", "image/jpg");
    private static final long MAX_SIZE = 5 * 1024 * 1024;

    /** DELETE /api/admin/menu-items — removes every menu item in one batch. */
    @DeleteMapping
    @Transactional
    public ResponseEntity<Void> deleteAllMenuItems() {
        // First detach all answers so the FK constraint doesn't block the delete
        menuItemRepository.detachAllAnswersFromMenuItems();
        menuItemRepository.deleteAllInBatch();
        return ResponseEntity.noContent().build();
    }

    /** DELETE /api/admin/menu-items/{id} — removes a single menu item by id. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMenuItem(@PathVariable Long id) {
        if (!menuItemRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        menuItemRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No file provided"));
        }
        if (file.getSize() > MAX_SIZE) {
            return ResponseEntity.badRequest().body(Map.of("error", "File too large (max 5MB)"));
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid file type. Use JPG, PNG or WEBP"));
        }

        String original = file.getOriginalFilename();
        String ext = ".webp";
        if (original != null && original.contains(".")) {
            String candidate = original.substring(original.lastIndexOf('.')).toLowerCase();
            if (Set.of(".jpg", ".jpeg", ".png", ".webp").contains(candidate)) {
                ext = candidate.equals(".jpeg") ? ".jpg" : candidate;
            }
        } else if ("image/png".equalsIgnoreCase(contentType)) {
            ext = ".png";
        } else if ("image/jpeg".equalsIgnoreCase(contentType)) {
            ext = ".jpg";
        }

        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(uploadPath);

        String filename = UUID.randomUUID().toString().replace("-", "") + ext;
        Path target = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        return ResponseEntity.ok(Map.of("imageName", filename, "url", "/uploads/" + filename));
    }
}
