package dev.pratyush.jitterbench;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Random;

final class ContentionSimulator {

    SimulationResult run(SimulationConfig config, BackoffStrategy strategy, long seed) {
        Random random = new Random(seed);
        PriorityQueue<Event> events = new PriorityQueue<>(Comparator
                .comparingDouble(Event::timeMs)
                .thenComparingLong(Event::sequence));
        long sequence = 0;
        int[] failures = new int[config.clients()];

        for (int client = 0; client < config.clients(); client++) {
            events.add(Event.readRequest(networkDelay(config, random), sequence++, client));
        }

        long resourceVersion = 0;
        long attempts = 0;
        int successes = 0;
        double completionMs = 0;

        while (!events.isEmpty()) {
            Event event = events.remove();
            completionMs = event.timeMs();

            switch (event.kind()) {
                case READ_REQUEST -> events.add(Event.readResponse(
                        event.timeMs() + networkDelay(config, random), sequence++,
                        event.client(), resourceVersion));
                case READ_RESPONSE -> events.add(Event.writeRequest(
                        event.timeMs() + networkDelay(config, random), sequence++,
                        event.client(), event.version()));
                case WRITE_REQUEST -> {
                    attempts++;
                    boolean won = event.version() == resourceVersion;
                    if (won) {
                        resourceVersion++;
                    }
                    events.add(Event.writeResponse(
                            event.timeMs() + networkDelay(config, random), sequence++,
                            event.client(), won));
                }
                case WRITE_RESPONSE -> {
                    if (event.success()) {
                        successes++;
                    } else {
                        int attempt = ++failures[event.client()];
                        double backoff = strategy.delayMillis(attempt, config, random);
                        events.add(Event.readRequest(
                                event.timeMs() + backoff + networkDelay(config, random),
                                sequence++, event.client()));
                    }
                }
            }
        }

        return new SimulationResult(strategy, config.clients(), attempts, successes, completionMs);
    }

    private double networkDelay(SimulationConfig config, Random random) {
        return Math.abs(config.networkMeanMs()
                + random.nextGaussian() * config.networkStddevMs());
    }

    private enum Kind { READ_REQUEST, READ_RESPONSE, WRITE_REQUEST, WRITE_RESPONSE }

    private record Event(
            double timeMs,
            long sequence,
            Kind kind,
            int client,
            long version,
            boolean success) {

        static Event readRequest(double time, long sequence, int client) {
            return new Event(time, sequence, Kind.READ_REQUEST, client, -1, false);
        }

        static Event readResponse(double time, long sequence, int client, long version) {
            return new Event(time, sequence, Kind.READ_RESPONSE, client, version, false);
        }

        static Event writeRequest(double time, long sequence, int client, long version) {
            return new Event(time, sequence, Kind.WRITE_REQUEST, client, version, false);
        }

        static Event writeResponse(double time, long sequence, int client, boolean success) {
            return new Event(time, sequence, Kind.WRITE_RESPONSE, client, -1, success);
        }
    }
}
