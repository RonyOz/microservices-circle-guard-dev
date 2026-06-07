package com.circleguard.file.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for FileStorageService — covers constructor (storage init),
 * successful persistence, and the failure path that wraps IO errors.
 */
class FileStorageServiceTest {

    private final FileStorageService service = new FileStorageService();

    @Test
    void saveFile_persistsAndReturnsPrefixedName() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "certificate.pdf", "application/pdf", "hello".getBytes());

        String stored = service.saveFile(file);

        assertThat(stored)
                .endsWith("_certificate.pdf")
                .matches("[0-9a-f\\-]{36}_certificate\\.pdf");
    }

    @Test
    void saveFile_whenStreamFails_throwsWrappedRuntimeException() throws IOException {
        MultipartFile broken = mock(MultipartFile.class);
        when(broken.getOriginalFilename()).thenReturn("broken.bin");
        when(broken.getInputStream()).thenThrow(new IOException("boom"));

        assertThatThrownBy(() -> service.saveFile(broken))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Could not store file");
    }

    @Test
    void loadFile_isNotYetImplemented_returnsNull() {
        assertThat(service.loadFile("anything")).isNull();
    }

    @Test
    void saveFile_collidesGracefully_whenSameNameStoredTwice() {
        MockMultipartFile a = new MockMultipartFile(
                "file", "dup.txt", "text/plain", "one".getBytes());
        MockMultipartFile b = new MockMultipartFile(
                "file", "dup.txt", "text/plain", "two".getBytes());

        // UUID prefix guarantees distinct stored names even for identical originals.
        assertThat(service.saveFile(a)).isNotEqualTo(service.saveFile(b));
    }
}
