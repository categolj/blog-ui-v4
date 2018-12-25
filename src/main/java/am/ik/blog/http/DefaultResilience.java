package am.ik.blog.http;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.function.Predicate;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.micrometer.CircuitBreakerMetrics;
import io.github.resilience4j.micrometer.RateLimiterMetrics;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.ratelimiter.operator.RateLimiterOperator;
import io.micrometer.core.instrument.MeterRegistry;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.retry.Backoff;
import reactor.retry.Retry;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuples;

import org.springframework.web.reactive.function.client.WebClientResponseException;

public class DefaultResilience {
	private static final Logger log = LoggerFactory.getLogger(DefaultResilience.class);

	private final CircuitBreakerRegistry circuitBreakerRegistry;
	private final RateLimiterRegistry rateLimiterRegistry;
	private final MeterRegistry meterRegistry;
	private final ConcurrentMap<String, Tuple3<CircuitBreakerOperator<?>, RateLimiterOperator<?>, Retry<String>>> registry = new ConcurrentHashMap<>();
	private final Predicate<Throwable> retryable = e -> {
		if (e instanceof WebClientResponseException) {
			return !((WebClientResponseException) e).getStatusCode().is4xxClientError();
		}
		else {
			return true;
		}
	};

	public DefaultResilience(CircuitBreakerRegistry circuitBreakerRegistry,
			RateLimiterRegistry rateLimiterRegistry, MeterRegistry meterRegistry) {
		this.circuitBreakerRegistry = circuitBreakerRegistry;
		this.rateLimiterRegistry = rateLimiterRegistry;
		this.meterRegistry = meterRegistry;
	}

	public DefaultResilience registerAll(String... names) {
		for (String name : names) {
			this.register(name);
		}
		this.circuitBreakerRegistry.getAllCircuitBreakers() //
				.forEach(circuitBreaker -> circuitBreaker.getEventPublisher()
						.onStateTransition(e -> log.info("{}", e)) //
						.onError(e -> log.error("{}", e)) //
						.onCallNotPermitted(e -> log.warn("CallNotPermitted: {}", e))
						.onReset(e -> log.info("{}", e)));
		CircuitBreakerMetrics.ofCircuitBreakerRegistry(this.circuitBreakerRegistry)
				.bindTo(this.meterRegistry);
		RateLimiterMetrics.ofRateLimiterRegistry(this.rateLimiterRegistry)
				.bindTo(this.meterRegistry);
		return this;
	}

	void register(String name) {
		CircuitBreakerOperator<?> circuitBreakerOperator = CircuitBreakerOperator
				.of(this.circuitBreakerRegistry.circuitBreaker(name,
						() -> CircuitBreakerConfig.custom() //
								.waitDurationInOpenState(Duration.ofSeconds(10)) //
								.failureRateThreshold(20) //
								.recordFailure(retryable) //
								.build()));

		RateLimiterOperator<?> rateLimiterOperator = RateLimiterOperator
				.of(this.rateLimiterRegistry.rateLimiter(name,
						() -> RateLimiterConfig.custom() //
								.timeoutDuration(Duration.ofSeconds(3)) //
								.limitRefreshPeriod(Duration.ofSeconds(1)) //
								.limitForPeriod(100) //
								.build()));

		Retry<String> retry = Retry
				.<String>onlyIf(ctx -> this.retryable.test(ctx.exception())) //
				.timeout(Duration.ofSeconds(6)) //
				.retryMax(3) //
				.backoff(Backoff.exponential(Duration.ofSeconds(1),
						Duration.ofSeconds(10), 2, false)) //
				.doOnRetry(ctx -> log.warn("Retrying {}({})", ctx.applicationContext(),
						ctx));
		this.registry.put(name,
				Tuples.of(circuitBreakerOperator, rateLimiterOperator, retry));
	}

	public <T> Function<Publisher<T>, Publisher<T>> all(String name) {
		return publisher -> {
			if (publisher instanceof Mono) {
				return ((Mono<T>) publisher).transform(this.retry(name))
						.transform(this.circuitBreaker(name));
			}
			else if (publisher instanceof Flux) {
				return ((Flux<T>) publisher).transform(this.retry(name))
						.transform(this.circuitBreaker(name));
			}
			return Mono.error(new IllegalStateException(
					"Publisher of type <" + publisher.getClass().getSimpleName()
							+ "> are not supported by this operator"));
		};
	}

	public <T> Function<Publisher<T>, Publisher<T>> retry(String name) {
		if (!this.registry.containsKey(name)) {
			log.warn("retry for {} has been registered at runtime.", name);
			this.register(name);
		}
		return publisher -> {
			Retry<String> retry = registry.get(name).getT3();
			if (publisher instanceof Mono) {
				return ((Mono<T>) publisher)
						.retryWhen(retry.withApplicationContext(name));
			}
			else if (publisher instanceof Flux) {
				return ((Flux<T>) publisher)
						.retryWhen(retry.withApplicationContext(name));
			}
			return Mono.error(new IllegalStateException(
					"Publisher of type <" + publisher.getClass().getSimpleName()
							+ "> are not supported by this operator"));
		};
	}

	@SuppressWarnings("unchecked")
	public <T> CircuitBreakerOperator<T> circuitBreaker(String name) {
		if (!this.registry.containsKey(name)) {
			log.warn("circuitBreaker {} has been registered at runtime.", name);
			this.register(name);
		}
		return (CircuitBreakerOperator<T>) this.registry.get(name).getT1();
	}

	@SuppressWarnings("unchecked")
	public <T> RateLimiterOperator<T> rateLimiter(String name) {
		if (!this.registry.containsKey(name)) {
			log.warn("rateLimiter {} has been registered at runtime.", name);
			this.register(name);
		}
		return (RateLimiterOperator<T>) this.registry.get(name).getT2();
	}

}
