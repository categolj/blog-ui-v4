package am.ik.blog.exception;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

import brave.Span;
import brave.Tracer;
import io.github.resilience4j.circuitbreaker.CircuitBreakerOpenException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.retry.RetryExhaustedException;

import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.server.ResponseStatusException;

@ControllerAdvice
public class BlogExceptionHandler {
	private final Logger log = LoggerFactory.getLogger(BlogExceptionHandler.class);
	private final Environment env;
	private final Tracer tracer;

	public BlogExceptionHandler(Environment env, Tracer tracer) {
		this.env = env;
		this.tracer = tracer;
	}

	@ExceptionHandler(ResponseStatusException.class)
	public Rendering handleResponseStatusException(ResponseStatusException e) {
		HttpStatus status = e.getStatus();
		this.logIfSeverError(e, status);
		return this.renderError(e, status);
	}

	@ExceptionHandler(WebClientResponseException.class)
	public Rendering handleWebClientResponseException(WebClientResponseException e) {
		HttpStatus status = e.getStatusCode();
		this.logIfSeverError(e, status);
		return this.renderError(e, status);
	}

	@ExceptionHandler(TimeoutException.class)
	public Rendering handleTimeoutException(TimeoutException e) {
		log.warn("Timeout occurred!", e);
		return this.renderError(e, HttpStatus.SERVICE_UNAVAILABLE);
	}

	@ExceptionHandler(CircuitBreakerOpenException.class)
	public Rendering handleCircuitBreakerOpenException(CircuitBreakerOpenException e) {
		log.warn("Circuit breaker is currently open!");
		return this.renderError(e, HttpStatus.SERVICE_UNAVAILABLE);
	}

	@ExceptionHandler(RetryExhaustedException.class)
	public Rendering handleRetryExhaustedException(RetryExhaustedException e) {
		log.warn("Retry exhausted!", e);
		return this.renderError(e.getCause(), HttpStatus.SERVICE_UNAVAILABLE);
	}

	@ExceptionHandler(Exception.class)
	public Rendering handleUnexpectedException(Exception e) {
		log.error("Unexpected exception occurred!", e);
		return this.renderError(e, HttpStatus.INTERNAL_SERVER_ERROR);
	}

	private void logIfSeverError(Exception e, HttpStatus statusCode) {
		if (statusCode.is5xxServerError()) {
			log.warn("Server error occurred! " + statusCode, e);
		}
	}

	private Rendering renderError(Throwable e, HttpStatus status) {
		Span span = this.tracer.currentSpan();
		Rendering.Builder builder = Rendering.view("error/error") //
				.modelAttribute("status", status.value()) //
				.modelAttribute("error", status.getReasonPhrase())
				.modelAttribute("message", Objects.toString(e.getMessage(), ""))
				.modelAttribute("b3", span == null ? null : span.context());
		if (env.acceptsProfiles(Profiles.of("default", "debug"))) {
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			builder = builder //
					.modelAttribute("exception", e.getClass().getName()) //
					.modelAttribute("trace", sw.toString());
		}
		return builder //
				.status(status) //
				.build();
	}
}
