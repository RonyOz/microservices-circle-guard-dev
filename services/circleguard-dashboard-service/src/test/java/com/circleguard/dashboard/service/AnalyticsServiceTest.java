package com.circleguard.dashboard.service;

import com.circleguard.dashboard.client.PromotionClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AnalyticsService — mocks JdbcTemplate, PromotionClient and the
 * KAnonymityFilter to exercise the feature-toggle, SQL masking and the time-series
 * fallback paths without a real database.
 */
class AnalyticsServiceTest {

    private JdbcTemplate jdbc;
    private PromotionClient promotionClient;
    private KAnonymityFilter kAnonymityFilter;
    private AnalyticsService service;

    @BeforeEach
    void setUp() {
        jdbc = mock(JdbcTemplate.class);
        promotionClient = mock(PromotionClient.class);
        kAnonymityFilter = mock(KAnonymityFilter.class);
        service = new AnalyticsService(jdbc, promotionClient, kAnonymityFilter);
        ReflectionTestUtils.setField(service, "kAnonymityEnabled", true);
    }

    @Test
    void getCampusSummary_delegatesToPromotionClient() {
        Map<String, Object> stats = Map.of("totalUsers", 10);
        when(promotionClient.getHealthStats()).thenReturn(stats);

        assertThat(service.getCampusSummary()).isEqualTo(stats);
        assertThat(service.getGlobalHealthStats()).isEqualTo(stats);
    }

    @Test
    void getDepartmentStats_whenToggleOn_appliesKAnonymity() {
        Map<String, Object> raw = Map.of("totalUsers", 3);
        Map<String, Object> masked = Map.of("totalUsers", "<5");
        when(promotionClient.getHealthStatsByDepartment("CS")).thenReturn(raw);
        when(kAnonymityFilter.apply(raw)).thenReturn(masked);

        assertThat(service.getDepartmentStats("CS")).isEqualTo(masked);
        verify(kAnonymityFilter).apply(raw);
    }

    @Test
    void getDepartmentStats_whenToggleOff_skipsKAnonymity() {
        ReflectionTestUtils.setField(service, "kAnonymityEnabled", false);
        Map<String, Object> raw = Map.of("totalUsers", 3);
        when(promotionClient.getHealthStatsByDepartment("CS")).thenReturn(raw);

        assertThat(service.getDepartmentStats("CS")).isEqualTo(raw);
        verifyNoInteractions(kAnonymityFilter);
    }

    @Test
    void getEntryTrends_masksBucketsBelowFive() {
        UUID loc = UUID.randomUUID();
        Map<String, Object> small = new HashMap<>(Map.of("hour", "10:00", "entry_count", 2L));
        Map<String, Object> big = new HashMap<>(Map.of("hour", "11:00", "entry_count", 40L));
        when(jdbc.queryForList(anyString(), eq(loc))).thenReturn(List.of(small, big));

        List<Map<String, Object>> out = service.getEntryTrends(loc);

        assertThat(out.get(0)).containsEntry("entry_count", "<5");
        assertThat(out.get(0)).containsEntry("note", "Insufficient data for privacy");
        assertThat(out.get(1)).containsEntry("entry_count", 40L);
    }

    @Test
    void getTimeSeries_onSqlError_returnsMockSeries() {
        when(jdbc.queryForList(anyString(), anyInt()))
                .thenThrow(new RuntimeException("table missing"));

        List<Map<String, Object>> out = service.getTimeSeries("hourly", 3);

        // fallback produces 4 statuses per bucket, min(limit,24) buckets → 3*4 = 12
        assertThat(out).hasSize(12);
        assertThat(out).allSatisfy(p -> assertThat(p).containsKeys("bucket", "status", "total"));
    }

    @Test
    void getTimeSeries_dailyPeriod_queriesSuccessfully() {
        List<Map<String, Object>> rows = List.of(Map.of("bucket", "day1", "status", "ACTIVE", "total", 5L));
        when(jdbc.queryForList(contains("day"), eq(10))).thenReturn(rows);

        assertThat(service.getTimeSeries("daily", 10)).isEqualTo(rows);
    }
}
