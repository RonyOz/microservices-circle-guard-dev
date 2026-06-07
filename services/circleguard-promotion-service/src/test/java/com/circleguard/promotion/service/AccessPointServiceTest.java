package com.circleguard.promotion.service;

import com.circleguard.promotion.model.AccessPoint;
import com.circleguard.promotion.model.Floor;
import com.circleguard.promotion.repository.jpa.AccessPointRepository;
import com.circleguard.promotion.repository.jpa.FloorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AccessPointService — access-point CRUD with mocked repositories.
 */
class AccessPointServiceTest {

    private AccessPointRepository accessPointRepository;
    private FloorRepository floorRepository;
    private AccessPointService service;

    @BeforeEach
    void setUp() {
        accessPointRepository = mock(AccessPointRepository.class);
        floorRepository = mock(FloorRepository.class);
        service = new AccessPointService(accessPointRepository, floorRepository);
        when(accessPointRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void registerAccessPoint_existingFloor_buildsAndSaves() {
        UUID fid = UUID.randomUUID();
        when(floorRepository.findById(fid)).thenReturn(Optional.of(Floor.builder().id(fid).build()));

        AccessPoint ap = service.registerAccessPoint(fid, "AA:BB:CC", 1.5, 2.5, "AP-1");

        assertThat(ap.getMacAddress()).isEqualTo("AA:BB:CC");
        assertThat(ap.getCoordinateX()).isEqualTo(1.5);
        assertThat(ap.getCoordinateY()).isEqualTo(2.5);
        assertThat(ap.getName()).isEqualTo("AP-1");
    }

    @Test
    void registerAccessPoint_missingFloor_throws() {
        UUID fid = UUID.randomUUID();
        when(floorRepository.findById(fid)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.registerAccessPoint(fid, "x", 0.0, 0.0, "n"))
                .hasMessageContaining("Floor not found");
    }

    @Test
    void getAccessPoint_andByFloor_delegate() {
        UUID id = UUID.randomUUID();
        when(accessPointRepository.findById(id)).thenReturn(Optional.of(new AccessPoint()));
        when(accessPointRepository.findByFloorId(id)).thenReturn(List.of(new AccessPoint(), new AccessPoint()));

        assertThat(service.getAccessPoint(id)).isPresent();
        assertThat(service.getAccessPointsByFloor(id)).hasSize(2);
    }

    @Test
    void updateAccessPoint_mutatesFields() {
        UUID id = UUID.randomUUID();
        when(accessPointRepository.findById(id)).thenReturn(Optional.of(new AccessPoint()));

        AccessPoint ap = service.updateAccessPoint(id, "DD:EE", 7.0, 8.0, "renamed");

        assertThat(ap.getMacAddress()).isEqualTo("DD:EE");
        assertThat(ap.getName()).isEqualTo("renamed");
    }

    @Test
    void updateAccessPoint_missing_throws() {
        UUID id = UUID.randomUUID();
        when(accessPointRepository.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.updateAccessPoint(id, "x", 0.0, 0.0, "n"))
                .hasMessageContaining("Access Point not found");
    }

    @Test
    void deleteAccessPoint_delegates() {
        UUID id = UUID.randomUUID();
        service.deleteAccessPoint(id);
        verify(accessPointRepository).deleteById(id);
    }
}
