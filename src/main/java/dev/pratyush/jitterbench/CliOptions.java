package dev.pratyush.jitterbench;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

record CliOptions(SimulationConfig config, int runs, long seed, Path output) {

    static CliOptions parse(String[] args) {
        Map<String, String> values = new HashMap<>();
        for (String arg : args) {
            if (!arg.startsWith("--") || !arg.contains("=")) {
                throw new IllegalArgumentException("Expected --name=value, received: " + arg);
            }
            String[] pair = arg.substring(2).split("=", 2);
            values.put(pair[0], pair[1]);
        }

        int clients = integer(values, "clients", 100);
        int runs = integer(values, "runs", 100);
        double mean = decimal(values, "network-mean-ms", 10);
        double variance = decimal(values, "network-variance-ms2", 4);
        long base = longValue(values, "base-backoff-ms", 5);
        long cap = longValue(values, "cap-backoff-ms", 2_000);
        long seed = longValue(values, "seed", 42);
        Path output = Path.of(values.getOrDefault("output", "benchmark.csv"));

        if (runs < 1 || variance < 0) {
            throw new IllegalArgumentException("runs must be positive and variance non-negative");
        }
        return new CliOptions(
                new SimulationConfig(clients, mean, Math.sqrt(variance), base, cap),
                runs, seed, output);
    }

    private static int integer(Map<String, String> values, String key, int fallback) {
        return Integer.parseInt(values.getOrDefault(key, Integer.toString(fallback)));
    }

    private static long longValue(Map<String, String> values, String key, long fallback) {
        return Long.parseLong(values.getOrDefault(key, Long.toString(fallback)));
    }

    private static double decimal(Map<String, String> values, String key, double fallback) {
        return Double.parseDouble(values.getOrDefault(key, Double.toString(fallback)));
    }
}
