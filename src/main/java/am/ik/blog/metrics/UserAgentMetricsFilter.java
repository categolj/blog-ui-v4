package am.ik.blog.metrics;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import is.tagomor.woothee.Classifier;
import is.tagomor.woothee.DataSet;
import reactor.core.publisher.Mono;

@Component
public class UserAgentMetricsFilter implements WebFilter {

	private final MeterRegistry meterRegistry;

	public UserAgentMetricsFilter(MeterRegistry registry) {
		this.meterRegistry = registry;
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		String path = exchange.getRequest().getPath().value();
		if (!isIgnored(path)) {
			String userAgent = exchange.getRequest().getHeaders()
					.getFirst(HttpHeaders.USER_AGENT);
			Map<String, String> r = Classifier.parse(userAgent);
			String name = r.get(DataSet.DATASET_KEY_NAME);
			String category = r.get(DataSet.DATASET_KEY_CATEGORY);
			List<Tag> tags = new ArrayList<>();
			if (!StringUtils.isEmpty(name)) {
				tags.add(Tag.of("name", name));
			}
			if (!StringUtils.isEmpty(category)) {
				tags.add(Tag.of("category", category));
			}
			this.meterRegistry.counter("useragent", tags).increment();
		}
		return chain.filter(exchange);
	}

	boolean isIgnored(String path) {
		return path.startsWith("/actuator/") || path.endsWith(".png")
				|| path.endsWith(".js") || path.endsWith(".css");
	}
}
