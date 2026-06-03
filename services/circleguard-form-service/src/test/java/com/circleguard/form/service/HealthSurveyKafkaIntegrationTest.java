package com.circleguard.form.service;

import com.circleguard.form.model.HealthSurvey;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cross-service integration test: form-service → promotion-service via Kafka.
 *
 * Verifies that HealthSurveyService.submitSurvey() publishes a correctly-formatted
 * event to "survey.submitted" which promotion-service's SurveyListener consumes.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:form_test;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.flyway.enabled=false"
        }
)
@EmbeddedKafka(
        partitions = 1,
        topics = {"survey.submitted"},
        bootstrapServersProperty = "spring.kafka.bootstrap-servers"
)
class HealthSurveyKafkaIntegrationTest {

    @Autowired
    private HealthSurveyService healthSurveyService;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    private Consumer<String, String> consumer;

    @BeforeEach
    void setUp() {
        Map<String, Object> props = KafkaTestUtils.consumerProps(
                "test-group-" + UUID.randomUUID(), "false", embeddedKafkaBroker);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        DefaultKafkaConsumerFactory<String, String> cf = new DefaultKafkaConsumerFactory<>(
                props, new StringDeserializer(), new StringDeserializer());
        consumer = cf.createConsumer();
        consumer.subscribe(List.of("survey.submitted"));
    }

    @AfterEach
    void tearDown() {
        consumer.close();
    }

    @Test
    void submitSurveyWithFever_publishesHasSymptomsTrue() {
        UUID anonymousId = UUID.randomUUID();
        HealthSurvey survey = HealthSurvey.builder()
                .anonymousId(anonymousId)
                .hasFever(true)
                .hasCough(false)
                .build();

        healthSurveyService.submitSurvey(survey);

        ConsumerRecord<String, String> record =
                KafkaTestUtils.getSingleRecord(consumer, "survey.submitted", 5_000);

        assertThat(record.key()).isEqualTo(anonymousId.toString());
        assertThat(record.value()).contains("\"hasSymptoms\":true");
        assertThat(record.value()).contains(anonymousId.toString());
    }

    @Test
    void submitSurveyWithCoughOnly_publishesHasSymptomsTrue() {
        UUID anonymousId = UUID.randomUUID();
        HealthSurvey survey = HealthSurvey.builder()
                .anonymousId(anonymousId)
                .hasFever(false)
                .hasCough(true)
                .build();

        healthSurveyService.submitSurvey(survey);

        ConsumerRecord<String, String> record =
                KafkaTestUtils.getSingleRecord(consumer, "survey.submitted", 5_000);

        assertThat(record.key()).isEqualTo(anonymousId.toString());
        assertThat(record.value()).contains("\"hasSymptoms\":true");
    }

    @Test
    void submitSurveyWithoutSymptoms_publishesHasSymptomsFalse() {
        UUID anonymousId = UUID.randomUUID();
        HealthSurvey survey = HealthSurvey.builder()
                .anonymousId(anonymousId)
                .hasFever(false)
                .hasCough(false)
                .build();

        healthSurveyService.submitSurvey(survey);

        ConsumerRecord<String, String> record =
                KafkaTestUtils.getSingleRecord(consumer, "survey.submitted", 5_000);

        assertThat(record.key()).isEqualTo(anonymousId.toString());
        assertThat(record.value()).contains("\"hasSymptoms\":false");
    }
}
