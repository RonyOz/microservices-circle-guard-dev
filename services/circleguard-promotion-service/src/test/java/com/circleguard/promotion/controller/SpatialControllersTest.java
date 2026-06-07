package com.circleguard.promotion.controller;

import com.circleguard.promotion.model.AccessPoint;
import com.circleguard.promotion.model.Building;
import com.circleguard.promotion.model.Floor;
import com.circleguard.promotion.service.AccessPointService;
import com.circleguard.promotion.service.BuildingService;
import com.circleguard.promotion.service.FloorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Web-layer tests for the spatial REST controllers (buildings/floors/access-points).
 * Security filters are disabled — these assert request mapping + service delegation only.
 */
class SpatialControllersTest {

    @WebMvcTest(BuildingController.class)
    @AutoConfigureMockMvc(addFilters = false)
    static class BuildingControllerTest {
        @Autowired MockMvc mockMvc;
        @MockBean BuildingService buildingService;
        @MockBean FloorService floorService;

        @Test
        void create_returnsBuilding() throws Exception {
            when(buildingService.createBuilding(any(), any(), any()))
                    .thenReturn(Building.builder().name("Lab").code("LB").build());
            mockMvc.perform(post("/api/v1/buildings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Lab\",\"code\":\"LB\",\"description\":\"d\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Lab"));
        }

        @Test
        void list_returnsBuildings() throws Exception {
            when(buildingService.getAllBuildings()).thenReturn(List.of(Building.builder().name("A").build()));
            mockMvc.perform(get("/api/v1/buildings"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].name").value("A"));
        }

        @Test
        void getFloors_andAddFloor() throws Exception {
            UUID id = UUID.randomUUID();
            when(floorService.getFloorsByBuilding(id)).thenReturn(List.of(new Floor()));
            mockMvc.perform(get("/api/v1/buildings/{id}/floors", id)).andExpect(status().isOk());

            when(floorService.addFloor(eq(id), anyInt(), any())).thenReturn(Floor.builder().floorNumber(2).build());
            mockMvc.perform(post("/api/v1/buildings/{id}/floors", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"floorNumber\":2,\"name\":\"Second\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.floorNumber").value(2));
        }

        @Test
        void update_andDelete() throws Exception {
            UUID id = UUID.randomUUID();
            when(buildingService.updateBuilding(eq(id), any(), any(), any()))
                    .thenReturn(Building.builder().name("New").build());
            mockMvc.perform(put("/api/v1/buildings/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"New\"}"))
                    .andExpect(status().isOk());

            mockMvc.perform(delete("/api/v1/buildings/{id}", id)).andExpect(status().isOk());
            verify(buildingService).deleteBuilding(id);
        }
    }

    @WebMvcTest(FloorController.class)
    @AutoConfigureMockMvc(addFilters = false)
    static class FloorControllerTest {
        @Autowired MockMvc mockMvc;
        @MockBean FloorService floorService;
        @MockBean AccessPointService accessPointService;

        @Test
        void addAccessPoint_andList() throws Exception {
            UUID id = UUID.randomUUID();
            when(accessPointService.registerAccessPoint(eq(id), any(), anyDouble(), anyDouble(), any()))
                    .thenReturn(AccessPoint.builder().macAddress("AA").build());
            mockMvc.perform(post("/api/v1/floors/{id}/access-points", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"macAddress\":\"AA\",\"coordinateX\":1,\"coordinateY\":2,\"name\":\"AP\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.macAddress").value("AA"));

            when(accessPointService.getAccessPointsByFloor(id)).thenReturn(List.of(new AccessPoint()));
            mockMvc.perform(get("/api/v1/floors/{id}/access-points", id)).andExpect(status().isOk());
        }

        @Test
        void update_andDelete() throws Exception {
            UUID id = UUID.randomUUID();
            when(floorService.updateFloor(eq(id), anyInt(), any(), any()))
                    .thenReturn(Floor.builder().floorNumber(9).build());
            mockMvc.perform(put("/api/v1/floors/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"floorNumber\":9,\"name\":\"N\",\"floorPlanUrl\":\"u\"}"))
                    .andExpect(status().isOk());

            mockMvc.perform(delete("/api/v1/floors/{id}", id)).andExpect(status().isOk());
            verify(floorService).deleteFloor(id);
        }
    }

    @WebMvcTest(AccessPointController.class)
    @AutoConfigureMockMvc(addFilters = false)
    static class AccessPointControllerTest {
        @Autowired MockMvc mockMvc;
        @MockBean AccessPointService accessPointService;

        @Test
        void get_present_andAbsent() throws Exception {
            UUID id = UUID.randomUUID();
            when(accessPointService.getAccessPoint(id)).thenReturn(Optional.of(AccessPoint.builder().macAddress("X").build()));
            mockMvc.perform(get("/api/v1/access-points/{id}", id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.macAddress").value("X"));

            UUID missing = UUID.randomUUID();
            when(accessPointService.getAccessPoint(missing)).thenReturn(Optional.empty());
            mockMvc.perform(get("/api/v1/access-points/{id}", missing)).andExpect(status().isNotFound());
        }

        @Test
        void update_andDelete() throws Exception {
            UUID id = UUID.randomUUID();
            when(accessPointService.updateAccessPoint(eq(id), any(), anyDouble(), anyDouble(), any()))
                    .thenReturn(AccessPoint.builder().name("renamed").build());
            mockMvc.perform(put("/api/v1/access-points/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"macAddress\":\"AA\",\"coordinateX\":1,\"coordinateY\":2,\"name\":\"renamed\"}"))
                    .andExpect(status().isOk());

            mockMvc.perform(delete("/api/v1/access-points/{id}", id)).andExpect(status().isOk());
            verify(accessPointService).deleteAccessPoint(id);
        }
    }
}
