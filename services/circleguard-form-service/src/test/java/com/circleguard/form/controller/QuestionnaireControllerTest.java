package com.circleguard.form.controller;

import com.circleguard.form.model.Questionnaire;
import com.circleguard.form.service.QuestionnaireService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(QuestionnaireController.class)
class QuestionnaireControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private QuestionnaireService service;

    @Test
    void getAll_returnsList() throws Exception {
        when(service.getAllQuestionnaires())
                .thenReturn(List.of(Questionnaire.builder().title("Daily").version(1).build()));

        mockMvc.perform(get("/api/v1/questionnaires"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Daily"));
    }

    @Test
    void getActive_present_returns200() throws Exception {
        when(service.getActiveQuestionnaire())
                .thenReturn(Optional.of(Questionnaire.builder().title("Active").version(2).build()));

        mockMvc.perform(get("/api/v1/questionnaires/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Active"));
    }

    @Test
    void getActive_absent_returns404() throws Exception {
        when(service.getActiveQuestionnaire()).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/questionnaires/active"))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_persistsAndReturnsQuestionnaire() throws Exception {
        when(service.saveQuestionnaire(any(Questionnaire.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/api/v1/questionnaires")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"New\",\"version\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("New"));
    }

    @Test
    void activate_returns200AndDelegates() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/questionnaires/{id}/activate", id))
                .andExpect(status().isOk());

        verify(service).activateQuestionnaire(id);
    }
}
