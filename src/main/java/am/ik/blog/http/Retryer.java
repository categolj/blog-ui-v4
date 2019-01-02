package am.ik.blog.http;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

import am.ik.blog.BlogProperties;
import brave.Span;
import brave.Tracer;
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
	public static final Predicate<Throwable> retryable = e -> {
		if (e instanceof WebClientResponseException) {
			return !((WebClientResponseException) e).getStatusCode().is4xxClientError();
		}
		else {
			return true;
		}
	};

	private final Tracer tracer;
	private final Retry<String> retry;

	public Retryer(Tracer tracer, BlogProperties.Retry props) {
		this.tracer = tracer;
		this.retry = Retry.<String>onlyIf(ctx -> retryable.test(ctx.exception())) //
				.retryMax(props.getMax()) //
				.backoff(Backoff.fixed(props.getFixedBackoff())) //
				.doOnRetry(ctx -> {
					Span span = this.tracer.currentSpan();
					span.tag("retry.iteration", String.valueOf(ctx.iteration()));
					Throwable exception = ctx.exception();
					if (exception != null) {
						span.tag("retry.exception", exception.getClass().getName());
						span.tag("retry.message",
								Objects.toString(exception.getMessage(), ""));
					}
					log.warn("Retrying name={} {}", ctx.applicationContext(), ctx);
				});
	}

	public <T> Function<Publisher<T>, Publisher<T>> retry(String name) {
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
