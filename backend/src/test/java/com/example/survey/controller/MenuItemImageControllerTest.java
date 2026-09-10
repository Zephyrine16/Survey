package com.example.survey.controller;

import com.example.survey.repository.MenuItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MenuItemImageControllerTest {

    private MenuItemRepository menuItemRepository;
    private MenuItemImageController controller;

    @TempDir
    Path tempUploadDir;

    @BeforeEach
    void setUp() {
        menuItemRepository = Mockito.mock(MenuItemRepository.class);
        controller = new MenuItemImageController(menuItemRepository);
        ReflectionTestUtils.setField(controller, "uploadDir", tempUploadDir.toString());
    }

    @Test
    void testUploadValidPngImage() throws IOException {
        // PNG magic bytes: 89 50 4E 47 0D 0A 1A 0A
        byte[] pngBytes = new byte[] {
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52
        };
        MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", pngBytes);

        ResponseEntity<?> response = controller.uploadImage(file);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(((Map<?, ?>) response.getBody()).containsKey("imageName"));
    }

    @Test
    void testUploadValidJpegImage() throws IOException {
        // JPEG magic bytes: FF D8 FF
        byte[] jpegBytes = new byte[] {
                (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01
        };
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", jpegBytes);

        ResponseEntity<?> response = controller.uploadImage(file);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(((Map<?, ?>) response.getBody()).containsKey("imageName"));
    }

    @Test
    void testUploadValidWebpImage() throws IOException {
        // WEBP magic bytes: RIFF....WEBP
        byte[] webpBytes = new byte[] {
                'R', 'I', 'F', 'F', 0x20, 0x00, 0x00, 0x00, 'W', 'E', 'B', 'P'
        };
        MockMultipartFile file = new MockMultipartFile("file", "test.webp", "image/webp", webpBytes);

        ResponseEntity<?> response = controller.uploadImage(file);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(((Map<?, ?>) response.getBody()).containsKey("imageName"));
    }

    @Test
    void testUploadMaliciousScriptMaskedAsPngIsRejected() throws IOException {
        // Text / script disguised with PNG content type and filename
        byte[] maliciousBytes = "<?php system($_GET['cmd']); ?>".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "exploit.png", "image/png", maliciousBytes);

        ResponseEntity<?> response = controller.uploadImage(file);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(((Map<?, ?>) response.getBody()).containsKey("error"));
        assertEquals("File content does not match a valid image signature", ((Map<?, ?>) response.getBody()).get("error"));
    }

    @Test
    void testUploadEmptyFileIsRejected() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);

        ResponseEntity<?> response = controller.uploadImage(file);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void testUploadInvalidMimeTypeIsRejected() throws IOException {
        byte[] pngBytes = new byte[] {
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D
        };
        MockMultipartFile file = new MockMultipartFile("file", "test.exe", "application/x-msdownload", pngBytes);

        ResponseEntity<?> response = controller.uploadImage(file);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
}
