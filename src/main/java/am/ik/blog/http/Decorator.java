package am.ik.blog.http;

import java.util.function.Function;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.cloud.circuitbreaker.commons.ReactiveCircuitBreaker;
import org.springframework.cloud.circuitbreaker.commons.ReactiveCircuitBreakerFactory;

public class Decorator {
	private final Retryer retryer;
	private final ReactiveCircuitBreakerFactory circuitBreakerFactory;

	public Decorator(Retryer retryer,
			ReactiveCircuitBreakerFactory circuitBreakerFactory) {
		this.retryer = retryer;
		this.circuitBreakerFactory = circuitBreakerFactory;
	}

	public <T> Function<Publisher<T>, Publisher<T>> decorate(String name) {
		ReactiveCircuitBreaker circuitBreaker = this.circuitBreakerFactory.create(name);
		return publisher -> {
			if (publisher instanceof Mono) {
				return ((Mono<T>) publisher).transform(this.retryer.retry(name)) //
						.transform(circuitBreaker::run);
			}
			else if (publisher instanceof Flux) {
				return ((Flux<T>) publisher).transform(this.retryer.retry(name)) //
						.transform(circuitBreaker::run);
			}
			throw new IllegalStateException(
					"Publisher of type <" + publisher.getClass().getSimpleName()
							+ "> are not supported by this operator");
		};
	}
}
