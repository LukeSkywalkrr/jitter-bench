package dev.pratyush.jitterbench;

record SimulationResult(
        BackoffStrategy strategy,
        int clients,
        long attempts,
        int successes,
        double completionMs) {
}
