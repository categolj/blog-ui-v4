package am.ik.blog.config;

import java.time.Duration;
import java.util.function.Supplier;

import am.ik.blog.BlogProperties;
import am.ik.blog.http.Retryer;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.vavr.collection.Seq;
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
			factory.configureCircuitBreakerRegistry(new CircuitBreakerRegistry() {
				private CircuitBreakerRegistry delegate = CircuitBreakerRegistry
						.ofDefaults();

				@Override
				public Seq<CircuitBreaker> getAllCircuitBreakers() {
					return this.delegate.getAllCircuitBreakers();
				}

				@Override
				public CircuitBreaker circuitBreaker(String name) {
					return this.customize(this.delegate.circuitBreaker(name));
				}

				@Override
				public CircuitBreaker circuitBreaker(String name,
						CircuitBreakerConfig circuitBreakerConfig) {
					return this.customize(
							this.delegate.circuitBreaker(name, circuitBreakerConfig));
				}

				@Override
				public CircuitBreaker circuitBreaker(String name,
						Supplier<CircuitBreakerConfig> circuitBreakerConfigSupplier) {
					return this.customize(this.delegate.circuitBreaker(name,
							circuitBreakerConfigSupplier));
				}

				private CircuitBreaker customize(CircuitBreaker circuitBreaker) {
					CircuitBreaker.EventPublisher eventPublisher = circuitBreaker
							.getEventPublisher();
					eventPublisher.onError(event -> log.error("[onError] {}", event));
					eventPublisher.onReset(event -> log.info("[onReset] {}", event));
					eventPublisher.onStateTransition(
							event -> log.info("[onStateTransition] {}", event));
					return circuitBreaker;
				}
			});
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
