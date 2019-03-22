package am.ik.blog.config;

import java.time.Duration;

import am.ik.blog.BlogProperties;
import am.ik.blog.http.Retryer;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.core.EventProcessor;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.circuitbreaker.commons.Customizer;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Resilience4JConfig {
	private final Logger log = LoggerFactory.getLogger(Resilience4JConfig.class);

	@Bean
	public ReactiveResilience4JCircuitBreakerFactory reactiveResilience4JCircuitBreakerFactory() {
		return new ReactiveResilience4JCircuitBreakerFactory();
	}

	@Bean
	public Customizer<ReactiveResilience4JCircuitBreakerFactory> resilience4jCustomizer(
			BlogProperties props) {
		return factory -> {
			factory.addCircuitBreakerCustomizer(circuitBreaker -> {
				CircuitBreaker.EventPublisher eventPublisher = circuitBreaker
						.getEventPublisher();
				if (!((EventProcessor) eventPublisher).hasConsumers()) {
					eventPublisher.onError(event -> log.error("[onError] {}", event));
					eventPublisher.onReset(event -> log.info("[onReset] {}", event));
					eventPublisher.onStateTransition(
							event -> log.info("[onStateTransition] {}", event));
				}
			}, "blog-ui.findById", "blog-ui.findAll", "blog-ui.streamAll",
					"blog-ui.findByQuery", "blog-ui.findByCategories",
					"blog-ui.findByTag", "blog-ui.findTags", "blog-ui.findCategories");
			factory.configureDefault(
					id -> new Resilience4JConfigBuilder(id)
							.timeLimiterConfig(TimeLimiterConfig.custom() //
									.timeoutDuration(props.getCircuitBreakerTimeout()) //
									.build())
							.circuitBreakerConfig(CircuitBreakerConfig.custom() //
									.waitDurationInOpenState(Duration.ofSeconds(10)) //
									.failureRateThreshold(40) //
									.ringBufferSizeInClosedState(20) //
									.ringBufferSizeInHalfOpenState(5) //
									.recordFailure(Retryer.retryable) //
									.build())
							.build());
		};
	}
}
