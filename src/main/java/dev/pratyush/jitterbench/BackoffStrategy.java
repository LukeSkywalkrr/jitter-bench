package dev.pratyush.jitterbench;

import java.util.random.RandomGenerator;

enum BackoffStrategy {
    NO_BACKOFF("no_backoff") {
        @Override
        double delayMillis(int attempt, SimulationConfig config, RandomGenerator random) {
            return 0;
        }
    },
    CAPPED_EXPONENTIAL("capped_exponential") {
        @Override
        double delayMillis(int attempt, SimulationConfig config, RandomGenerator random) {
            return ceilingMillis(attempt, config);
        }
    },
    FULL_JITTER("full_jitter") {
        @Override
        double delayMillis(int attempt, SimulationConfig config, RandomGenerator random) {
            return random.nextDouble(ceilingMillis(attempt, config));
        }
    };

    private final String csvName;

    BackoffStrategy(String csvName) {
        this.csvName = csvName;
    }

    String csvName() {
        return csvName;
    }

    abstract double delayMillis(int attempt, SimulationConfig config, RandomGenerator random);

    static double ceilingMillis(int attempt, SimulationConfig config) {
        double exponential = Math.scalb((double) config.baseBackoffMs(), attempt);
        return Math.min(config.capBackoffMs(), exponential);
    }
}
