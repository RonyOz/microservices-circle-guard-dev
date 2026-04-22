package com.circleguard.form.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;
import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "health_surveys")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class HealthSurvey {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "anonymous_id", nullable = false)
    private UUID anonymousId;

    @Column(name = "has_fever")
    private Boolean hasFever;

    @Column(name = "has_cough")
    private Boolean hasCough;

    @Column(name = "other_symptoms")
    private String otherSymptoms;

    @Column(name = "exposure_date")
    private LocalDate exposureDate;
}
