package am.ik.blog.exception;

import java.io.PrintWriter;
import java.io.StringWriter;

import com.netflix.hystrix.exception.HystrixRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.reactive.result.view.Rendering;

import static com.netflix.hystrix.exception.HystrixRuntimeException.FailureType.*;

@ControllerAdvice
public class HystrixExceptionHandler {
	private final Logger log = LoggerFactory.getLogger(HystrixExceptionHandler.class);
	private final Environment env;

	public HystrixExceptionHandler(Environment env) {
		this.env = env;
	}

	@ExceptionHandler(HystrixRuntimeException.class)
	public Rendering hystrix(HystrixRuntimeException e) {
		Throwable cause = e.getCause();
		HystrixRuntimeException.FailureType failureType = e.getFailureType();
		log.warn("{}({}:{}\t{}:{})", failureType, e.getClass().getName(), e.getMessage(),
				cause.getClass().getName(), cause.getMessage());
		if (failureType == REJECTED_SEMAPHORE_EXECUTION
				|| failureType == REJECTED_SEMAPHORE_FALLBACK
				|| failureType == REJECTED_THREAD_EXECUTION) {
			return this.renderError(e, HttpStatus.TOO_MANY_REQUESTS);
		}
		if (failureType == TIMEOUT) {
			return this.renderError(e, HttpStatus.REQUEST_TIMEOUT);
		}
		return this.renderError(e, HttpStatus.INTERNAL_SERVER_ERROR);
	}

	private Rendering renderError(Exception e, HttpStatus status) {
		Throwable cause = e.getCause();
		Rendering.Builder builder = Rendering.view("error/error") //
				.modelAttribute("status", status) //
				.modelAttribute("error", status.getReasonPhrase())
				.modelAttribute("message", cause.getMessage());
		if (env.acceptsProfiles("default", "debug")) {
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			builder = builder //
					.modelAttribute("exception", cause.getClass().getName()) //
					.modelAttribute("trace", sw.toString());
		}
		return builder //
				.status(status) //
				.build();
	}
}
