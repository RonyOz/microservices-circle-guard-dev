package com.circleguard.identity.service;

import com.circleguard.identity.model.IdentityMapping;
import com.circleguard.identity.repository.IdentityMappingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for IdentityVaultService — verifies deterministic hashing reuse,
 * new-mapping creation, and the not-found path of reverse resolution.
 */
class IdentityVaultServiceTest {

    private IdentityMappingRepository repository;
    private IdentityVaultService service;

    @BeforeEach
    void setUp() {
        repository = mock(IdentityMappingRepository.class);
        service = new IdentityVaultService(repository);
        ReflectionTestUtils.setField(service, "hashSalt", "test-salt");
    }

    @Test
    void getOrCreateAnonymousId_existingHash_returnsStoredIdWithoutSaving() {
        UUID existing = UUID.randomUUID();
        IdentityMapping mapping = IdentityMapping.builder().anonymousId(existing).build();
        when(repository.findByIdentityHash(anyString())).thenReturn(Optional.of(mapping));

        UUID result = service.getOrCreateAnonymousId("user@circleguard.edu");

        assertThat(result).isEqualTo(existing);
        verify(repository, never()).save(any());
    }

    @Test
    void getOrCreateAnonymousId_newHash_persistsAndReturnsGeneratedId() {
        UUID generated = UUID.randomUUID();
        when(repository.findByIdentityHash(anyString())).thenReturn(Optional.empty());
        when(repository.save(any(IdentityMapping.class))).thenAnswer(inv -> {
            IdentityMapping m = inv.getArgument(0);
            // simulate JPA assigning the anonymousId on persist
            ReflectionTestUtils.setField(m, "anonymousId", generated);
            return m;
        });

        UUID result = service.getOrCreateAnonymousId("new@circleguard.edu");

        assertThat(result).isEqualTo(generated);
        verify(repository).save(argThat(m ->
                m.getRealIdentity().equals("new@circleguard.edu")
                        && m.getIdentityHash() != null
                        && m.getSalt() != null));
    }

    @Test
    void getOrCreateAnonymousId_sameInput_producesSameHashLookup() {
        when(repository.findByIdentityHash(anyString())).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.getOrCreateAnonymousId("stable@circleguard.edu");
        service.getOrCreateAnonymousId("stable@circleguard.edu");

        // identical inputs must hit the repository with the identical hash
        verify(repository, times(2)).findByIdentityHash(argThat(h -> h.length() == 64));
    }

    @Test
    void resolveRealIdentity_found_returnsValue() {
        UUID id = UUID.randomUUID();
        IdentityMapping mapping = IdentityMapping.builder()
                .anonymousId(id).realIdentity("real@circleguard.edu").build();
        when(repository.findById(id)).thenReturn(Optional.of(mapping));

        assertThat(service.resolveRealIdentity(id)).isEqualTo("real@circleguard.edu");
    }

    @Test
    void resolveRealIdentity_missing_throwsNotFound() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolveRealIdentity(id))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Identity not found");
    }
}
