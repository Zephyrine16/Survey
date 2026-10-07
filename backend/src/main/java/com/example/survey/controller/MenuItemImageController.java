package com.example.survey.controller;

import com.example.survey.repository.MenuItemRepository;
import com.example.survey.repository.UploadedImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/menu-items")
@RequiredArgsConstructor
public class MenuItemImageController {

    private final MenuItemRepository menuItemRepository;
    private final UploadedImageRepository uploadedImageRepository;

    @Value("${security.maintenance.enabled:false}")
    private boolean maintenanceEnabled;

    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp", "image/jpg");
    private static final long MAX_SIZE = 5 * 1024 * 1024;

    /** DELETE /api/admin/menu-items/{id} — removes a single menu item by id. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMenuItem(@PathVariable Long id) {
        if (!maintenanceEnabled) return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).build();
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

        byte[] content = file.getBytes();
        String detectedContentType = detectImageContentType(content);
        if (detectedContentType == null || !detectedContentType.equals(
                "image/jpg".equalsIgnoreCase(contentType) ? "image/jpeg" : contentType.toLowerCase())) {
            return ResponseEntity.badRequest().body(Map.of("error", "File content does not match a valid image signature"));
        }

        String ext = switch (detectedContentType) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };

        String filename = UUID.randomUUID().toString().replace("-", "") + ext;
        uploadedImageRepository.save(filename, detectedContentType, content);

        return ResponseEntity.ok(Map.of("imageName", filename, "url", "/uploads/" + filename));
    }

    private static String detectImageContentType(byte[] header) {
        if (header == null || header.length < 12) {
            return null;
        }
        // JPEG: FF D8 FF
        if ((header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        // PNG: 89 50 4E 47 0D 0A 1A 0A
        if ((header[0] & 0xFF) == 0x89 && header[1] == 0x50 && header[2] == 0x4E && header[3] == 0x47
                && header[4] == 0x0D && header[5] == 0x0A && header[6] == 0x1A && header[7] == 0x0A) {
            return "image/png";
        }
        // WEBP: 'RIFF' .... 'WEBP'
        if (header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P') {
            return "image/webp";
        }
        return null;
    }
}
