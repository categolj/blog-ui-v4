package am.ik.blog;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "blog")
public class BlogProperties {
	private Service api;
	private Service point;
	private Retry retry = new Retry();
	private boolean debugHttp = false;
	private Duration connectTimeout = Duration.ofSeconds(1);
	private Duration circuitBreakerTimeout = Duration.ofSeconds(6);

	public Service getApi() {
		return api;
	}

	public void setApi(Service api) {
		this.api = api;
	}

	public Service getPoint() {
		return point;
	}

	public void setPoint(Service point) {
		this.point = point;
	}

	public Retry getRetry() {
		return retry;
	}

	public void setRetry(Retry retry) {
		this.retry = retry;
	}

	public boolean isDebugHttp() {
		return debugHttp;
	}

	public void setDebugHttp(boolean debugHttp) {
		this.debugHttp = debugHttp;
	}

	public Duration getConnectTimeout() {
		return connectTimeout;
	}

	public void setConnectTimeout(Duration connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	public Duration getCircuitBreakerTimeout() {
		return circuitBreakerTimeout;
	}

	public void setCircuitBreakerTimeout(Duration circuitBreakerTimeout) {
		this.circuitBreakerTimeout = circuitBreakerTimeout;
	}

	public static class Retry {
		private Duration fixedBackoff = Duration.ofSeconds(1);
		private int max = 3;

		public Duration getFixedBackoff() {
			return fixedBackoff;
		}

		public void setFixedBackoff(Duration fixedBackoff) {
			this.fixedBackoff = fixedBackoff;
		}

		public int getMax() {
			return max;
		}

		public void setMax(int max) {
			this.max = max;
		}
	}

	public static class Service {
		private String url;

		public String getUrl() {
			return url;
		}

		public void setUrl(String url) {
			this.url = url;
		}
	}
}
