package com.circleguard.form.service;

import com.circleguard.form.model.HealthSurvey;
import com.circleguard.form.repository.HealthSurveyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.SendResult;
import java.util.concurrent.CompletableFuture;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class HealthSurveyServiceTest {

    static class CapturingKafkaTemplate extends KafkaTemplate<String, Object> {
        public String capturedTopic;
        public String capturedKey;
        public Object capturedData;

        @SuppressWarnings("unchecked")
        public CapturingKafkaTemplate() {
            super((ProducerFactory<String, Object>) null);
        }

        @Override
        public CompletableFuture<SendResult<String, Object>> send(String topic, String key, Object data) {
            this.capturedTopic = topic;
            this.capturedKey = key;
            this.capturedData = data;
            return CompletableFuture.completedFuture(null);
        }
    }

    private HealthSurveyRepository repoThatReturnsSaved() {
        InvocationHandler handler = (proxy, method, args) -> {
            if ("save".equals(method.getName()) && args != null && args.length == 1) {
                HealthSurvey s = (HealthSurvey) args[0];
                if (s.getId() == null) s.setId(UUID.randomUUID());
                return s;
            }
            throw new UnsupportedOperationException("Not implemented in test stub: " + method.getName());
        };
        return (HealthSurveyRepository) Proxy.newProxyInstance(
                HealthSurveyRepository.class.getClassLoader(),
                new Class[]{HealthSurveyRepository.class},
                handler
        );
    }

    private HealthSurveyRepository repoThatThrows() {
        InvocationHandler handler = (proxy, method, args) -> {
            if ("save".equals(method.getName()) && args != null && args.length == 1) {
                throw new RuntimeException("DB is down");
            }
            throw new UnsupportedOperationException("Not implemented in test stub: " + method.getName());
        };
        return (HealthSurveyRepository) Proxy.newProxyInstance(
                HealthSurveyRepository.class.getClassLoader(),
                new Class[]{HealthSurveyRepository.class},
                handler
        );
    }

    @Test
    void submitSurvey_savesAndReturnsObject() {
        HealthSurveyRepository repo = repoThatReturnsSaved();
        CapturingKafkaTemplate kafka = new CapturingKafkaTemplate();
        HealthSurveyService svc = new HealthSurveyService(repo, kafka);

        HealthSurvey in = HealthSurvey.builder()
                .anonymousId(UUID.randomUUID())
                .hasFever(false)
                .hasCough(false)
                .otherSymptoms("none")
                .exposureDate(LocalDate.now())
                .build();

        HealthSurvey out = svc.submitSurvey(in);
        assertNotNull(out.getId(), "Saved survey should have an id assigned");
        assertEquals(in.getAnonymousId(), out.getAnonymousId());
    }

    @Test
    void submitSurvey_emitsKafkaEvent_hasSymptomsTrue() {
        HealthSurveyRepository repo = repoThatReturnsSaved();
        CapturingKafkaTemplate kafka = new CapturingKafkaTemplate();
        HealthSurveyService svc = new HealthSurveyService(repo, kafka);

        UUID anon = UUID.randomUUID();
        HealthSurvey s = HealthSurvey.builder()
                .anonymousId(anon)
                .hasFever(true)
                .hasCough(false)
                .build();

        HealthSurvey saved = svc.submitSurvey(s);

        assertEquals("survey.submitted", kafka.capturedTopic);
        assertEquals(anon.toString(), kafka.capturedKey);
        assertNotNull(kafka.capturedData);
        assertTrue(kafka.capturedData instanceof Map);
        Map<?, ?> event = (Map<?, ?>) kafka.capturedData;
        assertTrue(Boolean.TRUE.equals(event.get("hasSymptoms")));
        assertEquals(anon, event.get("anonymousId"));
        assertTrue(event.containsKey("timestamp"));
    }

    @Test
    void submitSurvey_emitsKafkaEvent_hasSymptomsFalse() {
        HealthSurveyRepository repo = repoThatReturnsSaved();
        CapturingKafkaTemplate kafka = new CapturingKafkaTemplate();
        HealthSurveyService svc = new HealthSurveyService(repo, kafka);

        UUID anon = UUID.randomUUID();
        HealthSurvey s = HealthSurvey.builder()
                .anonymousId(anon)
                .hasFever(false)
                .hasCough(false)
                .build();

        svc.submitSurvey(s);

        assertEquals("survey.submitted", kafka.capturedTopic);
        Map<?, ?> event = (Map<?, ?>) kafka.capturedData;
        assertFalse(Boolean.TRUE.equals(event.get("hasSymptoms")));
    }

    @Test
    void submitSurvey_repositoryError_propagates() {
        HealthSurveyRepository repo = repoThatThrows();
        CapturingKafkaTemplate kafka = new CapturingKafkaTemplate();
        HealthSurveyService svc = new HealthSurveyService(repo, kafka);

        HealthSurvey s = HealthSurvey.builder()
                .anonymousId(UUID.randomUUID())
                .hasFever(false)
                .hasCough(false)
                .build();

        RuntimeException ex = assertThrows(RuntimeException.class, () -> svc.submitSurvey(s));
        assertTrue(ex.getMessage().contains("DB is down"));
    }
}
