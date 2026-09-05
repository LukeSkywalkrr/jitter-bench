package dev.pratyush.jitterbench;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;

final class BenchmarkCommand {
    private final PrintStream out;

    BenchmarkCommand(PrintStream out) {
        this.out = out;
    }

    void run(String[] args) throws IOException {
        CliOptions options = CliOptions.parse(args);
        List<BenchmarkResult> results = new BenchmarkSuite(new ContentionSimulator())
                .run(options.config(), options.runs(), options.seed());
        String csv = csv(results, options.config());
        Files.writeString(options.output(), csv, StandardCharsets.UTF_8);

        out.println("Local deterministic contention simulation");
        out.printf(Locale.ROOT,
                "clients=%d runs=%d network_mean_ms=%.0f network_variance_ms2=%.0f base_ms=%d cap_ms=%d seed=%d%n",
                options.config().clients(), options.runs(), options.config().networkMeanMs(),
                options.config().networkVarianceMs2(), options.config().baseBackoffMs(),
                options.config().capBackoffMs(), options.seed());
        out.println("strategy              avg_attempts  avg_completion_ms");
        for (BenchmarkResult result : results) {
            out.printf(Locale.ROOT, "%-22s %12.2f %18.2f%n",
                    result.strategy().csvName(), result.averageAttempts(),
                    result.averageCompletionMs());
        }

        BenchmarkResult none = find(results, BackoffStrategy.NO_BACKOFF);
        BenchmarkResult exponential = find(results, BackoffStrategy.CAPPED_EXPONENTIAL);
        BenchmarkResult jitter = find(results, BackoffStrategy.FULL_JITTER);
        out.printf(Locale.ROOT, "Full jitter vs no backoff: %.2f%% fewer attempts%n",
                reduction(none.averageAttempts(), jitter.averageAttempts()));
        out.printf(Locale.ROOT, "Full jitter vs capped exponential: %.2f%% shorter completion%n",
                reduction(exponential.averageCompletionMs(), jitter.averageCompletionMs()));
        out.println("Wrote " + options.output());
    }

    static String csv(List<BenchmarkResult> results, SimulationConfig config) {
        BenchmarkResult none = find(results, BackoffStrategy.NO_BACKOFF);
        BenchmarkResult exponential = find(results, BackoffStrategy.CAPPED_EXPONENTIAL);
        StringBuilder csv = new StringBuilder(
                "strategy,clients,runs,network_mean_ms,network_variance_ms2,base_backoff_ms,cap_backoff_ms,avg_attempts,avg_completion_ms,attempt_reduction_vs_none_pct,completion_reduction_vs_exponential_pct\n");
        for (BenchmarkResult result : results) {
            csv.append(String.format(Locale.ROOT,
                    "%s,%d,%d,%.0f,%.0f,%d,%d,%.2f,%.2f,%.2f,%.2f%n",
                    result.strategy().csvName(), result.clients(), result.runs(),
                    config.networkMeanMs(), config.networkVarianceMs2(),
                    config.baseBackoffMs(), config.capBackoffMs(),
                    result.averageAttempts(), result.averageCompletionMs(),
                    reduction(none.averageAttempts(), result.averageAttempts()),
                    reduction(exponential.averageCompletionMs(), result.averageCompletionMs())));
        }
        return csv.toString();
    }

    private static BenchmarkResult find(List<BenchmarkResult> results, BackoffStrategy strategy) {
        return results.stream().filter(result -> result.strategy() == strategy)
                .findFirst().orElseThrow();
    }

    private static double reduction(double baseline, double candidate) {
        return (baseline - candidate) * 100.0 / baseline;
    }
}
