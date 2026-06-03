package com.circleguard.notification.service;

import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Cross-service integration test: promotion-service → notification-service via Kafka.
 *
 * Simulates promotion-service publishing to "promotion.status.changed" and verifies
 * that ExposureNotificationListener processes the event and calls NotificationDispatcher.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@EmbeddedKafka(
        partitions = 1,
        topics = {"promotion.status.changed"},
        bootstrapServersProperty = "spring.kafka.bootstrap-servers"
)
class ExposureNotificationListenerIntegrationTest {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @MockBean
    private NotificationDispatcher dispatcher;

    private KafkaTemplate<String, String> testProducer;

    @BeforeEach
    void setUp() {
        Map<String, Object> producerProps = KafkaTestUtils.producerProps(embeddedKafkaBroker);
        ProducerFactory<String, String> pf = new DefaultKafkaProducerFactory<>(
                producerProps, new StringSerializer(), new StringSerializer());
        testProducer = new KafkaTemplate<>(pf);
    }

    @AfterEach
    void tearDown() {
        testProducer.destroy();
    }

    @Test
    void whenStatusChangedWithUserId_dispatcherCalledWithCorrectUser() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        doAnswer(inv -> { latch.countDown(); return null; })
                .when(dispatcher).dispatch(anyString(), anyString());

        testProducer.send("promotion.status.changed",
                "{\"userId\":\"alice-uuid-001\",\"status\":\"SUSPECT\",\"timestamp\":1234567890}");

        assertTrue(latch.await(10, TimeUnit.SECONDS),
                "ExposureNotificationListener did not call dispatch within 10 seconds");
        verify(dispatcher).dispatch(eq("alice-uuid-001"), anyString());
    }

    @Test
    void whenEventMissingUserId_dispatcherCalledWithFallback() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        doAnswer(inv -> { latch.countDown(); return null; })
                .when(dispatcher).dispatch(anyString(), anyString());

        testProducer.send("promotion.status.changed",
                "{\"anonymousId\":\"no-userid-here\",\"status\":\"CONFIRMED\"}");

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        verify(dispatcher).dispatch(eq("unknown-user"), anyString());
    }

    @Test
    void whenMultipleStatusChanges_allEventsAreDispatched() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(3);
        doAnswer(inv -> { latch.countDown(); return null; })
                .when(dispatcher).dispatch(anyString(), anyString());

        testProducer.send("promotion.status.changed", "{\"userId\":\"user-1\",\"status\":\"SUSPECT\"}");
        testProducer.send("promotion.status.changed", "{\"userId\":\"user-2\",\"status\":\"PROBABLE\"}");
        testProducer.send("promotion.status.changed", "{\"userId\":\"user-3\",\"status\":\"CONFIRMED\"}");

        assertTrue(latch.await(10, TimeUnit.SECONDS),
                "Expected 3 dispatch calls within 10 seconds");
        verify(dispatcher, times(3)).dispatch(anyString(), anyString());
    }
}
