package dev.pratyush.jitterbench;

import java.util.ArrayList;
import java.util.List;

final class BenchmarkSuite {
    private final ContentionSimulator simulator;

    BenchmarkSuite(ContentionSimulator simulator) {
        this.simulator = simulator;
    }

    List<BenchmarkResult> run(SimulationConfig config, int runs, long seed) {
        List<BenchmarkResult> results = new ArrayList<>();
        for (BackoffStrategy strategy : BackoffStrategy.values()) {
            long attempts = 0;
            double completionMs = 0;
            for (int run = 0; run < runs; run++) {
                SimulationResult result = simulator.run(config, strategy, seed + run);
                if (result.successes() != config.clients()) {
                    throw new IllegalStateException("Not every client completed");
                }
                attempts += result.attempts();
                completionMs += result.completionMs();
            }
            results.add(new BenchmarkResult(strategy, config.clients(), runs,
                    attempts / (double) runs, completionMs / runs));
        }
        return List.copyOf(results);
    }
}
