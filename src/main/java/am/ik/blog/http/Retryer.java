package am.ik.blog.http;

import java.time.Duration;
import java.util.function.Function;
import java.util.function.Predicate;

import brave.Span;
import brave.Tracer;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.retry.Backoff;
import reactor.retry.Retry;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import org.springframework.web.reactive.function.client.WebClientResponseException;

public class Retryer {
	private static final Logger log = LoggerFactory.getLogger(Retryer.class);
	public static final Duration TIMEOUT = Duration.ofSeconds(8);
	public static final Predicate<Throwable> retryable = e -> {
		if (e instanceof WebClientResponseException) {
			return !((WebClientResponseException) e).getStatusCode().is4xxClientError();
		}
		else {
			return true;
		}
	};

	private static final Retry<Tuple2<String, Tracer>> retry = Retry
			.<Tuple2<String, Tracer>>onlyIf(ctx -> retryable.test(ctx.exception())) //
			.timeout(TIMEOUT) //
			.retryMax(3) //
			.backoff(Backoff.exponential(Duration.ofSeconds(1), Duration.ofSeconds(10), 2,
					false)) //
			.doOnRetry(ctx -> {
				Tuple2<String, Tracer> tpl = ctx.applicationContext();
				Span span = tpl.getT2().currentSpan();
				span.tag("retry.iteration", String.valueOf(ctx.iteration()));
				Throwable exception = ctx.exception();
				if (exception != null) {
					span.tag("retry.exception", exception.getClass().getName());
					span.tag("retry.message", exception.getMessage());
				}
				log.warn("Retrying name={} {}", tpl.getT1(), ctx);
			});

	public static <T> Function<Publisher<T>, Publisher<T>> retry(String name,
			Tracer tracer) {
		return publisher -> {
			if (publisher instanceof Mono) {
				return ((Mono<T>) publisher)
						.retryWhen(retry.withApplicationContext(Tuples.of(name, tracer)));
			}
			else if (publisher instanceof Flux) {
				return ((Flux<T>) publisher)
						.retryWhen(retry.withApplicationContext(Tuples.of(name, tracer)));
			}
			throw new IllegalStateException(
					"Publisher of type <" + publisher.getClass().getSimpleName()
							+ "> are not supported by this operator");
		};
	}
}
