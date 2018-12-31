package am.ik.blog.config;

import java.time.Duration;

import am.ik.blog.http.Retryer;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;

import org.springframework.cloud.circuitbreaker.r4j.R4JConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class R4JConfig {
	@Bean
	public R4JConfigFactory r4JConfigFactory() {
		return new R4JConfigFactory.DefaultR4JConfigFactory() {

			@Override
			public TimeLimiterConfig getTimeLimiterConfig(String id) {
				return TimeLimiterConfig.custom() //
						.timeoutDuration(Retryer.TIMEOUT) //
						.build();
			}

			@Override
			public CircuitBreakerConfig getDefaultCircuitBreakerConfig() {
				return CircuitBreakerConfig.custom() //
						.waitDurationInOpenState(Duration.ofSeconds(10)) //
						.failureRateThreshold(20) //
						.recordFailure(Retryer.retryable) //
						.build();
			}
		};
	}
}
