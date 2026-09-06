package dev.pratyush.jitterbench;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "jitter.benchmark.enabled=false")
class JitterBenchApplicationTest {

    @Test
    void springBootContextStarts() {
    }
}
