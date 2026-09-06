package dev.pratyush.jitterbench;

record BenchmarkResult(
        BackoffStrategy strategy,
        int clients,
        int runs,
        double averageAttempts,
        double averageCompletionMs) {
}
