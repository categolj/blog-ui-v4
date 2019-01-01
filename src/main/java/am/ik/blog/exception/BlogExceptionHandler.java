package am.ik.blog.exception;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	public BlogExceptionHandler(Environment env) {
		this.env = env;
	}

	@ExceptionHandler(ResponseStatusException.class)
	public Rendering handleResponseStatusException(ResponseStatusException e) {
		return this.renderError(e, e.getStatus());
	}

	@ExceptionHandler(WebClientResponseException.class)
	public Rendering handleWebClientResponseException(WebClientResponseException e) {
		return this.renderError(e, e.getStatusCode());
	}

	@ExceptionHandler(RuntimeException.class)
	public Rendering handleUnexpectedException(RuntimeException e) {
		log.error("Unexpected exception occurred!", e);
		return this.renderError(e, HttpStatus.INTERNAL_SERVER_ERROR);
	}

	private Rendering renderError(Exception e, HttpStatus status) {
		Rendering.Builder builder = Rendering.view("error/error") //
				.modelAttribute("status", status) //
				.modelAttribute("error", status.getReasonPhrase())
				.modelAttribute("message", Objects.toString(e.getMessage(), ""));
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