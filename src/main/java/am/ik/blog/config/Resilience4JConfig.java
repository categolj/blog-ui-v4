package am.ik.blog.config;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import am.ik.blog.BlogProperties;
import am.ik.blog.http.Retryer;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.core.EventProcessor;
import io.github.resilience4j.micrometer.tagged.TagNames;
import io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetrics;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
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
			MeterRegistry registry, BlogProperties props) {
		ConcurrentMap<String, Boolean> registered = new ConcurrentHashMap<>();
		return factory -> {
			factory.addCircuitBreakerCustomizer(circuitBreaker -> {
				//this.configureMetrics(registry, circuitBreaker, registered);
				this.configureEventPublisher(circuitBreaker.getEventPublisher());
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

	private void configureMetrics(MeterRegistry registry, CircuitBreaker circuitBreaker,
			ConcurrentMap<String, Boolean> registered) {
		TaggedCircuitBreakerMetrics.MetricNames names = TaggedCircuitBreakerMetrics.MetricNames
				.ofDefaults();
		String circuitBreakerName = circuitBreaker.getName();
		if (!registered.containsKey(circuitBreakerName)) {
			registered.putIfAbsent(circuitBreakerName, true);
			log.info("Register metric for {}", circuitBreaker);
			Gauge.builder(names.getStateMetricName(), circuitBreaker,
					(cb) -> cb.getState().getOrder())
					.tag(TagNames.NAME, circuitBreakerName).register(registry);
			Gauge.builder(names.getCallsMetricName(), circuitBreaker,
					(cb) -> cb.getMetrics().getNumberOfFailedCalls())
					.tag(TagNames.NAME, circuitBreakerName).tag(TagNames.KIND, "failed")
					.register(registry);
			Gauge.builder(names.getCallsMetricName(), circuitBreaker,
					(cb) -> cb.getMetrics().getNumberOfNotPermittedCalls())
					.tag(TagNames.NAME, circuitBreakerName)
					.tag(TagNames.KIND, "not_permitted").register(registry);
			Gauge.builder(names.getCallsMetricName(), circuitBreaker,
					(cb) -> cb.getMetrics().getNumberOfSuccessfulCalls())
					.tag(TagNames.NAME, circuitBreakerName)
					.tag(TagNames.KIND, "successful").register(registry);
			Gauge.builder(names.getBufferedCallsMetricName(), circuitBreaker,
					(cb) -> cb.getMetrics().getNumberOfBufferedCalls())
					.tag(TagNames.NAME, circuitBreakerName).register(registry);
			Gauge.builder(names.getMaxBufferedCallsMetricName(), circuitBreaker,
					(cb) -> cb.getMetrics().getMaxNumberOfBufferedCalls())
					.tag(TagNames.NAME, circuitBreakerName).register(registry);
		}
	}

	private void configureEventPublisher(CircuitBreaker.EventPublisher eventPublisher) {
		if (!((EventProcessor) eventPublisher).hasConsumers()) {
			eventPublisher.onError(event -> log.error("[onError] {}", event));
			eventPublisher.onReset(event -> log.info("[onReset] {}", event));
			eventPublisher.onStateTransition(
					event -> log.info("[onStateTransition] {}", event));
		}
	}
}
