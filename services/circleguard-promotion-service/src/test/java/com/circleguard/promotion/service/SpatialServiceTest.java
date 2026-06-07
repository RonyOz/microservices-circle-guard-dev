package com.circleguard.promotion.service;

import com.circleguard.promotion.model.AccessPoint;
import com.circleguard.promotion.model.Building;
import com.circleguard.promotion.model.Floor;
import com.circleguard.promotion.repository.jpa.AccessPointRepository;
import com.circleguard.promotion.repository.jpa.BuildingRepository;
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
 * Unit tests for SpatialService — CRUD over buildings/floors/access-points with
 * mocked JPA repositories. Covers happy paths plus the not-found and
 * referential-integrity guard branches.
 */
class SpatialServiceTest {

    private BuildingRepository buildingRepository;
    private FloorRepository floorRepository;
    private AccessPointRepository accessPointRepository;
    private SpatialService service;

    @BeforeEach
    void setUp() {
        buildingRepository = mock(BuildingRepository.class);
        floorRepository = mock(FloorRepository.class);
        accessPointRepository = mock(AccessPointRepository.class);
        service = new SpatialService(buildingRepository, floorRepository, accessPointRepository);
        when(buildingRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(floorRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(accessPointRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void createBuilding_buildsAndSaves() {
        Building b = service.createBuilding("Lab A", "LA", "desc");
        assertThat(b.getName()).isEqualTo("Lab A");
        assertThat(b.getCode()).isEqualTo("LA");
        verify(buildingRepository).save(any(Building.class));
    }

    @Test
    void addFloor_existingBuilding_linksAndSaves() {
        UUID bid = UUID.randomUUID();
        Building b = Building.builder().id(bid).build();
        when(buildingRepository.findById(bid)).thenReturn(Optional.of(b));

        Floor f = service.addFloor(bid, 3, "Third");

        assertThat(f.getBuilding()).isSameAs(b);
        assertThat(f.getFloorNumber()).isEqualTo(3);
    }

    @Test
    void addFloor_missingBuilding_throws() {
        UUID bid = UUID.randomUUID();
        when(buildingRepository.findById(bid)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.addFloor(bid, 1, "x"))
                .hasMessageContaining("Building not found");
    }

    @Test
    void getAllBuildings_andFloorsByBuilding_delegate() {
        when(buildingRepository.findAll()).thenReturn(List.of(new Building()));
        UUID bid = UUID.randomUUID();
        when(floorRepository.findByBuildingId(bid)).thenReturn(List.of(new Floor()));

        assertThat(service.getAllBuildings()).hasSize(1);
        assertThat(service.getFloorsByBuilding(bid)).hasSize(1);
    }

    @Test
    void updateBuilding_mutatesFields() {
        UUID id = UUID.randomUUID();
        when(buildingRepository.findById(id)).thenReturn(Optional.of(Building.builder().id(id).build()));

        Building updated = service.updateBuilding(id, "New", "NW", "newdesc");

        assertThat(updated.getName()).isEqualTo("New");
        assertThat(updated.getCode()).isEqualTo("NW");
    }

    @Test
    void updateBuilding_missing_throws() {
        UUID id = UUID.randomUUID();
        when(buildingRepository.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.updateBuilding(id, "a", "b", "c"))
                .hasMessageContaining("Building not found");
    }

    @Test
    void deleteBuilding_withFloors_isBlocked() {
        UUID id = UUID.randomUUID();
        when(floorRepository.findByBuildingId(id)).thenReturn(List.of(new Floor()));
        assertThatThrownBy(() -> service.deleteBuilding(id))
                .hasMessageContaining("existing floors");
        verify(buildingRepository, never()).deleteById(any());
    }

    @Test
    void deleteBuilding_empty_deletes() {
        UUID id = UUID.randomUUID();
        when(floorRepository.findByBuildingId(id)).thenReturn(List.of());
        service.deleteBuilding(id);
        verify(buildingRepository).deleteById(id);
    }

    @Test
    void updateFloor_mutatesFields_andMissingThrows() {
        UUID id = UUID.randomUUID();
        when(floorRepository.findById(id)).thenReturn(Optional.of(Floor.builder().id(id).build()));
        Floor f = service.updateFloor(id, 5, "Fifth");
        assertThat(f.getFloorNumber()).isEqualTo(5);
        assertThat(f.getName()).isEqualTo("Fifth");

        UUID missing = UUID.randomUUID();
        when(floorRepository.findById(missing)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.updateFloor(missing, 1, "x"))
                .hasMessageContaining("Floor not found");
    }

    @Test
    void deleteFloor_withAccessPoints_isBlocked_elseDeletes() {
        UUID withAps = UUID.randomUUID();
        when(accessPointRepository.findByFloorId(withAps)).thenReturn(List.of(new AccessPoint()));
        assertThatThrownBy(() -> service.deleteFloor(withAps))
                .hasMessageContaining("existing access points");

        UUID empty = UUID.randomUUID();
        when(accessPointRepository.findByFloorId(empty)).thenReturn(List.of());
        service.deleteFloor(empty);
        verify(floorRepository).deleteById(empty);
    }

    @Test
    void registerAccessPoint_existingFloor_buildsAp_missingThrows() {
        UUID fid = UUID.randomUUID();
        when(floorRepository.findById(fid)).thenReturn(Optional.of(Floor.builder().id(fid).build()));
        AccessPoint ap = service.registerAccessPoint(fid, "AA:BB", 1.0, 2.0, "AP1");
        assertThat(ap.getMacAddress()).isEqualTo("AA:BB");
        assertThat(ap.getCoordinateX()).isEqualTo(1.0);

        UUID missing = UUID.randomUUID();
        when(floorRepository.findById(missing)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.registerAccessPoint(missing, "x", 0.0, 0.0, "n"))
                .hasMessageContaining("Floor not found");
    }

    @Test
    void getAccessPoint_andByFloor_delegate() {
        UUID id = UUID.randomUUID();
        when(accessPointRepository.findById(id)).thenReturn(Optional.of(new AccessPoint()));
        when(accessPointRepository.findByFloorId(id)).thenReturn(List.of(new AccessPoint()));
        assertThat(service.getAccessPoint(id)).isPresent();
        assertThat(service.getAccessPointsByFloor(id)).hasSize(1);
    }

    @Test
    void updateAccessPoint_mutates_missingThrows() {
        UUID id = UUID.randomUUID();
        when(accessPointRepository.findById(id)).thenReturn(Optional.of(new AccessPoint()));
        AccessPoint ap = service.updateAccessPoint(id, "CC:DD", 9.0, 8.0, "renamed");
        assertThat(ap.getName()).isEqualTo("renamed");
        assertThat(ap.getCoordinateY()).isEqualTo(8.0);

        UUID missing = UUID.randomUUID();
        when(accessPointRepository.findById(missing)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.updateAccessPoint(missing, "x", 0.0, 0.0, "n"))
                .hasMessageContaining("Access Point not found");
    }

    @Test
    void deleteAccessPoint_delegates() {
        UUID id = UUID.randomUUID();
        service.deleteAccessPoint(id);
        verify(accessPointRepository).deleteById(id);
    }
}
