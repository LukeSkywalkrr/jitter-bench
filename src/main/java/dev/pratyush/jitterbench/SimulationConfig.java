package dev.pratyush.jitterbench;

record SimulationConfig(
        int clients,
        double networkMeanMs,
        double networkStddevMs,
        long baseBackoffMs,
        long capBackoffMs) {

    SimulationConfig {
        if (clients < 1 || networkMeanMs < 0 || networkStddevMs < 0
                || baseBackoffMs < 0 || capBackoffMs < baseBackoffMs) {
            throw new IllegalArgumentException("Invalid simulation configuration");
        }
    }

    double networkVarianceMs2() {
        return networkStddevMs * networkStddevMs;
    }
}
