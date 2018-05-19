package am.ik.blog.exception;

import java.io.PrintWriter;
import java.io.StringWriter;

import com.netflix.hystrix.exception.HystrixRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.reactive.result.view.Rendering;

import static com.netflix.hystrix.exception.HystrixRuntimeException.FailureType.*;

@ControllerAdvice
public class HystrixExceptionHandler {
	private final Logger log = LoggerFactory.getLogger(HystrixExceptionHandler.class);

	@ExceptionHandler(HystrixRuntimeException.class)
	public Rendering hystrix(HystrixRuntimeException e) {
		Throwable cause = e.getCause();
		HystrixRuntimeException.FailureType failureType = e.getFailureType();
		log.warn("{}({}:{})", failureType, cause.getClass().getName(), e.getMessage());
		if (failureType == REJECTED_SEMAPHORE_EXECUTION
				|| failureType == REJECTED_SEMAPHORE_FALLBACK
				|| failureType == REJECTED_THREAD_EXECUTION) {
			return this.renderError(e, HttpStatus.TOO_MANY_REQUESTS);
		}
		if (failureType == TIMEOUT) {
			return this.renderError(e, HttpStatus.REQUEST_TIMEOUT);
		}
		return this.renderError(e, HttpStatus.NOT_ACCEPTABLE);
	}

	private Rendering renderError(Exception e, HttpStatus status) {
		Throwable cause = e.getCause();
		StringWriter sw = new StringWriter();
		e.printStackTrace(new PrintWriter(sw));
		return Rendering.view("error/error") //
				.modelAttribute("status", status) //
				.modelAttribute("error", status.getReasonPhrase()) //
				.modelAttribute("exception", cause.getClass().getName()) //
				.modelAttribute("message", cause.getMessage()) //
				.modelAttribute("trace", sw.toString()) //
				.status(status) //
				.build();
	}
}
