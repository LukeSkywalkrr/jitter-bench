package dev.pratyush.jitterbench;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;

class ContentionSimulatorTest {
    private final ContentionSimulator simulator = new ContentionSimulator();
    private final SimulationConfig defaults = new SimulationConfig(100, 10, 2, 5, 2_000);

    @Test
    void everyContendingClientEventuallySucceeds() {
        for (BackoffStrategy strategy : BackoffStrategy.values()) {
            SimulationResult result = simulator.run(defaults, strategy, 42);
            assertThat(result.successes()).isEqualTo(100);
            assertThat(result.attempts()).isGreaterThanOrEqualTo(100);
        }
    }

    @Test
    void oneClientNeedsOneAttemptWithEveryStrategy() {
        SimulationConfig noContention = new SimulationConfig(1, 10, 2, 5, 2_000);
        for (BackoffStrategy strategy : BackoffStrategy.values()) {
            assertThat(simulator.run(noContention, strategy, 42).attempts()).isEqualTo(1);
        }
    }

    @Test
    void aFixedSeedIsExactlyRepeatable() {
        SimulationResult first = simulator.run(defaults, BackoffStrategy.FULL_JITTER, 42);
        SimulationResult second = simulator.run(defaults, BackoffStrategy.FULL_JITTER, 42);
        assertThat(second).isEqualTo(first);
    }

    @Test
    void fullJitterReducesWorkUnderHigherContention() {
        SimulationConfig crowded = new SimulationConfig(200, 10, 2, 5, 2_000);
        List<BenchmarkResult> results = new BenchmarkSuite(simulator).run(crowded, 40, 100);
        double noBackoff = attempts(results, BackoffStrategy.NO_BACKOFF);
        double fullJitter = attempts(results, BackoffStrategy.FULL_JITTER);
        assertThat(fullJitter).isLessThan(noBackoff);
    }

    @Test
    void delaysRespectTheExponentialCap() {
        var random = new java.util.Random(7);
        for (int attempt = 0; attempt < 80; attempt++) {
            double fixed = BackoffStrategy.CAPPED_EXPONENTIAL
                    .delayMillis(attempt, defaults, random);
            double jittered = BackoffStrategy.FULL_JITTER
                    .delayMillis(attempt, defaults, random);
            assertThat(fixed).isBetween(0.0, 2_000.0);
            assertThat(jittered).isBetween(0.0, fixed);
        }
    }

    @Test
    void independentRunsRemainIsolatedOnVirtualThreads() throws Exception {
        SimulationResult expected = simulator.run(defaults, BackoffStrategy.FULL_JITTER, 42);
        List<Callable<SimulationResult>> tasks = new ArrayList<>();
        for (int index = 0; index < 32; index++) {
            tasks.add(() -> simulator.run(defaults, BackoffStrategy.FULL_JITTER, 42));
        }
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (var future : executor.invokeAll(tasks)) {
                assertThat(future.get()).isEqualTo(expected);
            }
        }
    }

    private double attempts(List<BenchmarkResult> results, BackoffStrategy strategy) {
        return results.stream().filter(result -> result.strategy() == strategy)
                .findFirst().orElseThrow().averageAttempts();
    }
}
