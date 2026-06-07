package com.circleguard.promotion.listener;

import com.circleguard.promotion.service.HealthStatusService;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Cross-service integration test (consumer side): form-service → promotion-service via Kafka.
 *
 * Counterpart to form-service's HealthSurveyKafkaIntegrationTest (which asserts the event is
 * PUBLISHED). Here we boot only the Kafka listener wiring + the real SurveyListener against an
 * embedded broker, publish a "survey.submitted" event, and assert the listener CONSUMES it and
 * drives HealthStatusService — without loading promotion's Neo4j/PostgreSQL/Redis stack.
 */
@SpringBootTest(classes = SurveyListenerKafkaIntegrationTest.TestKafkaConfig.class)
@EmbeddedKafka(partitions = 1, topics = {"survey.submitted"},
        bootstrapServersProperty = "spring.kafka.bootstrap-servers")
class SurveyListenerKafkaIntegrationTest {

    @MockBean
    private HealthStatusService healthStatusService;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    void surveyWithSymptoms_isConsumed_andPromotesToSuspect() {
        Map<String, Object> event = new HashMap<>();
        event.put("anonymousId", "anon-42");
        event.put("hasSymptoms", true);

        kafkaTemplate.send("survey.submitted", "anon-42", event);
        kafkaTemplate.flush();

        // listener runs async on a consumer thread → wait for the side effect
        verify(healthStatusService, timeout(10_000)).updateStatus("anon-42", "SUSPECT");
    }

    @Test
    void surveyWithoutSymptoms_isConsumed_butDoesNotPromote() {
        Map<String, Object> event = new HashMap<>();
        event.put("anonymousId", "anon-7");
        event.put("hasSymptoms", false);

        kafkaTemplate.send("survey.submitted", "anon-7", event);
        kafkaTemplate.flush();

        // give the consumer time to process, then assert no promotion happened
        verify(healthStatusService, after(2_000).never()).updateStatus(anyString(), anyString());
    }

    /**
     * Minimal Kafka slice: real SurveyListener + producer/consumer factories bound to the
     * embedded broker. Avoids the full SpringBootApplication (and its datasource beans).
     */
    @EnableKafka
    @Configuration
    static class TestKafkaConfig {

        @Bean
        SurveyListener surveyListener(HealthStatusService healthStatusService) {
            return new SurveyListener(healthStatusService);
        }

        @Bean
        ProducerFactory<String, Object> producerFactory(EmbeddedKafkaBroker broker) {
            Map<String, Object> props = new HashMap<>();
            props.put("bootstrap.servers", broker.getBrokersAsString());
            props.put("key.serializer", StringSerializer.class);
            props.put("value.serializer", JsonSerializer.class);
            return new DefaultKafkaProducerFactory<>(props);
        }

        @Bean
        KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> pf) {
            return new KafkaTemplate<>(pf);
        }

        @Bean
        ConsumerFactory<String, Object> consumerFactory(EmbeddedKafkaBroker broker) {
            Map<String, Object> props = new HashMap<>();
            props.put("bootstrap.servers", broker.getBrokersAsString());
            props.put(ConsumerConfig.GROUP_ID_CONFIG, "promotion-service-group");
            props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
            props.put("key.deserializer", org.apache.kafka.common.serialization.StringDeserializer.class);
            JsonDeserializer<Object> valueDeserializer = new JsonDeserializer<>(Object.class);
            valueDeserializer.addTrustedPackages("*");
            return new DefaultKafkaConsumerFactory<>(props,
                    new org.apache.kafka.common.serialization.StringDeserializer(), valueDeserializer);
        }

        @Bean
        ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
                ConsumerFactory<String, Object> cf) {
            ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                    new ConcurrentKafkaListenerContainerFactory<>();
            factory.setConsumerFactory(cf);
            return factory;
        }
    }
}
