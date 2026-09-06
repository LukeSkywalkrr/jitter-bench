package dev.pratyush.jitterbench;

import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

@SpringBootApplication(proxyBeanMethods = false)
public class JitterBenchApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(JitterBenchApplication.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        application.setBannerMode(Banner.Mode.OFF);
        application.setDefaultProperties(java.util.Map.of("logging.level.root", "OFF"));
        application.run(args);
    }

    @Bean
    @ConditionalOnProperty(name = "jitter.benchmark.enabled", matchIfMissing = true)
    CommandLineRunner runBenchmark() {
        return args -> new BenchmarkCommand(System.out).run(args);
    }
}
