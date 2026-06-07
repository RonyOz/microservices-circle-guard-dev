package com.circleguard.form.service;

import com.circleguard.form.model.Question;
import com.circleguard.form.model.Questionnaire;
import com.circleguard.form.repository.QuestionnaireRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for QuestionnaireService — verifies parent-child wiring on save and
 * the single-active-version invariant enforced by activateQuestionnaire.
 */
class QuestionnaireServiceTest {

    private QuestionnaireRepository repository;
    private QuestionnaireService service;

    @BeforeEach
    void setUp() {
        repository = mock(QuestionnaireRepository.class);
        service = new QuestionnaireService(repository);
    }

    @Test
    void getAllQuestionnaires_delegatesToRepository() {
        List<Questionnaire> all = List.of(new Questionnaire());
        when(repository.findAll()).thenReturn(all);
        assertThat(service.getAllQuestionnaires()).isEqualTo(all);
    }

    @Test
    void getActiveQuestionnaire_returnsLatestActive() {
        Questionnaire active = Questionnaire.builder().version(3).isActive(true).build();
        when(repository.findFirstByIsActiveTrueOrderByVersionDesc()).thenReturn(Optional.of(active));
        assertThat(service.getActiveQuestionnaire()).contains(active);
    }

    @Test
    void saveQuestionnaire_backlinksEachQuestionToParent() {
        Question q1 = Question.builder().text("fever?").build();
        Question q2 = Question.builder().text("cough?").build();
        Questionnaire questionnaire = Questionnaire.builder()
                .title("Daily check").questions(List.of(q1, q2)).build();
        when(repository.save(any(Questionnaire.class))).thenAnswer(inv -> inv.getArgument(0));

        Questionnaire saved = service.saveQuestionnaire(questionnaire);

        assertThat(q1.getQuestionnaire()).isSameAs(questionnaire);
        assertThat(q2.getQuestionnaire()).isSameAs(questionnaire);
        assertThat(saved).isSameAs(questionnaire);
    }

    @Test
    void saveQuestionnaire_withNullQuestions_doesNotFail() {
        Questionnaire questionnaire = Questionnaire.builder().title("Empty").build();
        when(repository.save(any(Questionnaire.class))).thenAnswer(inv -> inv.getArgument(0));
        assertThat(service.saveQuestionnaire(questionnaire)).isSameAs(questionnaire);
    }

    @Test
    void activateQuestionnaire_deactivatesPreviousActiveThenActivatesTarget() {
        UUID targetId = UUID.randomUUID();
        Questionnaire current = Questionnaire.builder().id(UUID.randomUUID()).isActive(true).build();
        Questionnaire inactive = Questionnaire.builder().id(UUID.randomUUID()).isActive(false).build();
        Questionnaire target = Questionnaire.builder().id(targetId).isActive(false).build();

        when(repository.findAll()).thenReturn(List.of(current, inactive));
        when(repository.findById(targetId)).thenReturn(Optional.of(target));

        service.activateQuestionnaire(targetId);

        assertThat(current.getIsActive()).isFalse();
        assertThat(target.getIsActive()).isTrue();
        // current was deactivated (save) + target activated (save) = 2 saves
        verify(repository, times(2)).save(any(Questionnaire.class));
    }

    @Test
    void activateQuestionnaire_unknownId_onlyDeactivates() {
        UUID missing = UUID.randomUUID();
        when(repository.findAll()).thenReturn(List.of());
        when(repository.findById(missing)).thenReturn(Optional.empty());

        service.activateQuestionnaire(missing);

        verify(repository, never()).save(any());
    }
}
