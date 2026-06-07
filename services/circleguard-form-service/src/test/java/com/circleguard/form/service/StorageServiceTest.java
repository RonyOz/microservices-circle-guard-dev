package com.circleguard.form.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for StorageService — local filesystem persistence of attachments.
 */
class StorageServiceTest {

    private final StorageService service = new StorageService();

    @Test
    void store_persistsFileWithUuidPrefix() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "report.pdf", "application/pdf", "data".getBytes());

        String stored = service.store(file);

        assertThat(stored).endsWith("_report.pdf");
    }

    @Test
    void store_whenInputStreamFails_throwsRuntimeException() throws IOException {
        MultipartFile broken = mock(MultipartFile.class);
        when(broken.getOriginalFilename()).thenReturn("x.bin");
        when(broken.getInputStream()).thenThrow(new IOException("io failure"));

        assertThatThrownBy(() -> service.store(broken))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Could not store the file");
    }
}
