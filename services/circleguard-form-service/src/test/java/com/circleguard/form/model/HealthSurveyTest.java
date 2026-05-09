package com.circleguard.form.model;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

public class HealthSurveyTest {

    @Test
    void builder_setsFields() {
        UUID anon = UUID.randomUUID();
        HealthSurvey s = HealthSurvey.builder()
                .anonymousId(anon)
                .hasFever(true)
                .hasCough(false)
                .otherSymptoms("none")
                .exposureDate(LocalDate.now())
                .build();

        assertEquals(anon, s.getAnonymousId());
        assertTrue(Boolean.TRUE.equals(s.getHasFever()));
        assertFalse(Boolean.TRUE.equals(s.getHasCough()));
    }
}
