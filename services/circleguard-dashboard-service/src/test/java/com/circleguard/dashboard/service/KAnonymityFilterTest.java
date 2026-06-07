package com.circleguard.dashboard.service;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the k-anonymity masking engine (k=5 default).
 * Verifies group suppression for small cohorts and pass-through for safe sizes.
 */
class KAnonymityFilterTest {

    private final KAnonymityFilter filter = new KAnonymityFilter();

    @Test
    void apply_nullInput_returnsEmptyMap() {
        assertThat(filter.apply(null)).isEmpty();
    }

    @Test
    void apply_totalUsersBelowK_masksWholeGroupButKeepsDeptAndTimestamp() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalUsers", 3);
        stats.put("department", "Physics");
        stats.put("timestamp", "2026-06-06T10:00:00Z");
        stats.put("confirmedCount", 2);

        Map<String, Object> out = filter.apply(stats);

        assertThat(out).containsEntry("totalUsers", "<5");
        assertThat(out).containsEntry("department", "Physics");
        assertThat(out).containsEntry("timestamp", "2026-06-06T10:00:00Z");
        assertThat(out).containsEntry("note", "Insufficient data for privacy");
        // raw counts must not leak when whole group is masked
        assertThat(out).doesNotContainKey("confirmedCount");
    }

    @Test
    void apply_totalUsersSafe_masksOnlyIndividualSmallCounts() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalUsers", 50);
        stats.put("confirmedCount", 2);   // < k → masked
        stats.put("suspectCount", 12);    // >= k → kept
        stats.put("healthyCount", 0);     // zero → kept (not < k by rule count>0)

        Map<String, Object> out = filter.apply(stats);

        assertThat(out).containsEntry("confirmedCount", "<5");
        assertThat(out).containsEntry("suspectCount", 12);
        assertThat(out).containsEntry("healthyCount", 0);
        assertThat(out).containsEntry("totalUsers", 50);
    }

    @Test
    void apply_customK_appliesThreshold() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalUsers", 8);
        stats.put("exposedCount", 8);

        Map<String, Object> out = filter.apply(stats, 10);

        assertThat(out).containsEntry("totalUsers", "<10");
        assertThat(out).containsEntry("note", "Insufficient data for privacy");
    }

    @Test
    void apply_noTotalUsersKey_leavesCountsExceptSmallOnes() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("activeCount", 100);
        stats.put("probableCount", 1);

        Map<String, Object> out = filter.apply(stats);

        assertThat(out).containsEntry("activeCount", 100);
        assertThat(out).containsEntry("probableCount", "<5");
    }
}
