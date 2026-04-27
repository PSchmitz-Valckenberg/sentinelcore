package com.sentinelcore.service;

import com.sentinelcore.dto.AggregatedStrategyMetrics;
import com.sentinelcore.dto.RunMetricsResponse;
import com.sentinelcore.dto.RunMetricsResponse.SecurityMetrics;
import com.sentinelcore.dto.RunMetricsResponse.UtilityMetrics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class BenchmarkStatisticsTest {

    private static RunMetricsResponse metrics(double asr, double fpr, double rr, double lat) {
        return new RunMetricsResponse(
                "run-id", "DEFENDED", "COMPLETED",
                new UtilityMetrics(25, 0, 0, 25, asr, 0.0, lat),
                new SecurityMetrics(0, 0, fpr, rr),
                Map.of()
        );
    }

    @Test
    @DisplayName("mean of empty array returns 0")
    void meanOfEmpty() {
        assertThat(BenchmarkService.mean(new double[]{})).isEqualTo(0.0);
    }

    @Test
    @DisplayName("mean of single value equals that value")
    void meanOfSingle() {
        assertThat(BenchmarkService.mean(new double[]{0.4})).isEqualTo(0.4);
    }

    @Test
    @DisplayName("mean of multiple values is correct")
    void meanOfMultiple() {
        assertThat(BenchmarkService.mean(new double[]{0.1, 0.3, 0.5}))
                .isCloseTo(0.3, within(0.001));
    }

    @Test
    @DisplayName("stddev of single value returns null")
    void stddevOfSingleIsNull() {
        assertThat(BenchmarkService.stddev(new double[]{0.5})).isNull();
    }

    @Test
    @DisplayName("stddev of identical values is 0")
    void stddevOfIdenticalValues() {
        assertThat(BenchmarkService.stddev(new double[]{0.2, 0.2, 0.2}))
                .isNotNull()
                .isCloseTo(0.0, within(0.001));
    }

    @Test
    @DisplayName("stddev of known values matches expected population stddev")
    void stddevKnownValues() {
        // population stddev of {0.0, 0.5, 1.0} = sqrt(((0.25+0+0.25)/3)) = sqrt(1/6) ≈ 0.4082
        Double result = BenchmarkService.stddev(new double[]{0.0, 0.5, 1.0});
        assertThat(result).isNotNull().isCloseTo(0.408, within(0.001));
    }

    @Test
    @DisplayName("aggregate of N=1 run: repetitions=1, stddevs are null")
    void aggregateN1() {
        AggregatedStrategyMetrics agg = BenchmarkService.aggregate(
                List.of(metrics(0.2, 0.0, 0.1, 1500.0)));

        assertThat(agg.repetitions()).isEqualTo(1);
        assertThat(agg.attackSuccessRateMean()).isEqualTo(0.2);
        assertThat(agg.attackSuccessRateStddev()).isNull();
        assertThat(agg.falsePositiveRateMean()).isEqualTo(0.0);
        assertThat(agg.falsePositiveRateStddev()).isNull();
        assertThat(agg.avgLatencyMsMean()).isEqualTo(1500.0);
        assertThat(agg.avgLatencyMsStddev()).isNull();
    }

    @Test
    @DisplayName("aggregate of N=3 runs: correct mean and non-null stddev")
    void aggregateN3() {
        List<RunMetricsResponse> runs = List.of(
                metrics(0.0, 0.0, 0.2, 1000.0),
                metrics(0.2, 0.0, 0.2, 1500.0),
                metrics(0.4, 0.0, 0.2, 2000.0)
        );
        AggregatedStrategyMetrics agg = BenchmarkService.aggregate(runs);

        assertThat(agg.repetitions()).isEqualTo(3);
        assertThat(agg.attackSuccessRateMean()).isCloseTo(0.2, within(0.001));
        assertThat(agg.attackSuccessRateStddev()).isNotNull().isCloseTo(0.163, within(0.001));
        assertThat(agg.falsePositiveRateStddev()).isNotNull().isCloseTo(0.0, within(0.001));
        assertThat(agg.avgLatencyMsMean()).isCloseTo(1500.0, within(0.1));
        assertThat(agg.avgLatencyMsStddev()).isNotNull().isCloseTo(408.0, within(1.0));
    }
}
