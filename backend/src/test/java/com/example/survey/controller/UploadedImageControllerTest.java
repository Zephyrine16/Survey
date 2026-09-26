package com.example.survey.controller;

import com.example.survey.repository.UploadedImageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UploadedImageControllerTest {
    @TempDir
    Path tempUploadDir;

    @Test
    void servesUploadedImageFromDatabaseAfterLocalFileIsGone() throws IOException {
        UploadedImageRepository repository = mock(UploadedImageRepository.class);
        byte[] content = new byte[] {1, 2, 3};
        String filename = "0123456789abcdef0123456789abcdef.png";
        when(repository.findByFilename(filename)).thenReturn(Optional.of(
                new UploadedImageRepository.UploadedImage("image/png", content)));
        UploadedImageController controller = new UploadedImageController(repository);
        ReflectionTestUtils.setField(controller, "uploadDir", tempUploadDir.toString());

        ResponseEntity<byte[]> response = controller.getImage(filename);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("image/png", response.getHeaders().getContentType().toString());
        assertArrayEquals(content, response.getBody());
    }

    @Test
    void servesLegacyImageFromLocalVolumeWhenDatabaseHasNoCopy() throws IOException {
        UploadedImageRepository repository = mock(UploadedImageRepository.class);
        String filename = "0123456789abcdef0123456789abcdef.webp";
        byte[] content = new byte[] {4, 5, 6};
        Files.write(tempUploadDir.resolve(filename), content);
        UploadedImageController controller = new UploadedImageController(repository);
        ReflectionTestUtils.setField(controller, "uploadDir", tempUploadDir.toString());

        ResponseEntity<byte[]> response = controller.getImage(filename);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("image/webp", response.getHeaders().getContentType().toString());
        assertArrayEquals(content, response.getBody());
    }

    @Test
    void rejectsUnsafeFilename() throws IOException {
        UploadedImageRepository repository = mock(UploadedImageRepository.class);
        UploadedImageController controller = new UploadedImageController(repository);
        ReflectionTestUtils.setField(controller, "uploadDir", tempUploadDir.toString());

        assertEquals(HttpStatus.NOT_FOUND, controller.getImage("..%2Fsecret.png").getStatusCode());
        verifyNoInteractions(repository);
    }
}
