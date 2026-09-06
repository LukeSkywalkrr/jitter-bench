# Full Jitter Spreads Retry Storms

A Java 21 Spring Boot simulator reproduces the contended optimistic-write model from AWS’s [Exponential Backoff And Jitter](https://aws.amazon.com/blogs/architecture/exponential-backoff-and-jitter/).

## The problem

Capped exponential backoff reduces call frequency, but clients that fail together still calculate the same sleep and wake together. In AWS’s model, N contending clients require N completion rounds and total work grows with N².

```text
same failure time + same backoff = same retry time
same retry time + one winner      = another collision
```

Full jitter changes the fixed sleep into an independently sampled delay:

```text
ceiling = min(cap, base × 2^attempt)
sleep   = random_between(0, ceiling)
```

## Run it

Requires Java 21. The checked-in Gradle wrapper downloads the pinned build tool automatically.

```bash
./gradlew bootRun
```

On Windows PowerShell, use `\.\gradlew.bat bootRun`.

The default command starts 100 clients together, averages 100 deterministic runs per strategy, prints the comparison, and writes `benchmark.csv`.

```bash
./gradlew bootRun --args="--clients=100 --runs=100 --network-mean-ms=10 --network-variance-ms2=4 --base-backoff-ms=5 --cap-backoff-ms=2000 --seed=42 --output=benchmark.csv"
```

## What it shows

The three rows compare no backoff, capped exponential backoff without jitter, and full jitter. `avg_attempts` measures client work. `avg_completion_ms` measures simulated time until every client receives its successful response.

With the checked-in defaults, the local simulator produced:

```text
strategy              avg_attempts  avg_completion_ms
no_backoff                  2422.21            2029.38
capped_exponential          1868.00           63778.62
full_jitter                  796.24            4910.71
```

That is 67.13% fewer attempts than no backoff and 92.30% shorter completion than capped exponential without jitter.

The default parameters follow AWS’s article and linked simulator. This repository’s exact results are generated locally and are not measurements of an AWS production system.

## Test

```bash
./gradlew test
```

The tests cover simultaneous clients, zero contention, deterministic replay, higher contention, capped delays, concurrent independent simulations on virtual threads, and Spring Boot startup.

## Limits

This is a deterministic discrete-event model, not a load test and not AWS’s production implementation. It preserves AWS’s optimistic-write mechanism and timing distributions, but it does not model sockets, thread pools, rate limits, partial outages, or downstream side effects.

The simulator assumes retries are safe. Jitter changes when a retry happens; it does not make a non-idempotent operation safe, choose a retry budget, or replace server-side admission control. Completion time is simulated time, not a wall-clock latency benchmark.
