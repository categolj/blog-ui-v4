package am.ik.blog;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "blog")
@Component
public class BlogProperties {
	private Service api;
	private Service point;
	private Duration retryFirstBackoff = Duration.ofSeconds(1);
	private int retryMax = 3;

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

	public Duration getRetryFirstBackoff() {
		return retryFirstBackoff;
	}

	public void setRetryFirstBackoff(Duration retryFirstBackoff) {
		this.retryFirstBackoff = retryFirstBackoff;
	}

	public int getRetryMax() {
		return retryMax;
	}

	public void setRetryMax(int retryMax) {
		this.retryMax = retryMax;
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
