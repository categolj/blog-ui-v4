package am.ik.blog.http;

import java.time.Duration;
import java.util.function.Function;
import java.util.function.Predicate;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.retry.Backoff;
import reactor.retry.Retry;

import org.springframework.web.reactive.function.client.WebClientResponseException;

public class Retryer {
	private static final Logger log = LoggerFactory.getLogger(Retryer.class);
	public static final Duration TIMEOUT = Duration.ofSeconds(6);
	public static final Predicate<Throwable> retryable = e -> {
		if (e instanceof WebClientResponseException) {
			return !((WebClientResponseException) e).getStatusCode().is4xxClientError();
		}
		else {
			return true;
		}
	};

	private static final Retry<String> retry = Retry
			.<String>onlyIf(ctx -> retryable.test(ctx.exception())) //
			.timeout(TIMEOUT) //
			.retryMax(3) //
			.backoff(Backoff.exponential(Duration.ofSeconds(1), Duration.ofSeconds(10), 2,
					false)) //
			.doOnRetry(ctx -> log.warn("Retrying {}({})", ctx.applicationContext(), ctx));

	public static <T> Function<Publisher<T>, Publisher<T>> retry(String name) {
		return publisher -> {
			if (publisher instanceof Mono) {
				return ((Mono<T>) publisher)
						.retryWhen(retry.withApplicationContext(name));
			}
			else if (publisher instanceof Flux) {
				return ((Flux<T>) publisher)
						.retryWhen(retry.withApplicationContext(name));
			}
			throw new IllegalStateException(
					"Publisher of type <" + publisher.getClass().getSimpleName()
							+ "> are not supported by this operator");
		};
	}
}
