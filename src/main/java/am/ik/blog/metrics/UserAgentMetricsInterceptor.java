package am.ik.blog.metrics;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import is.tagomor.woothee.Classifier;
import is.tagomor.woothee.DataSet;

@Component
public class UserAgentMetricsInterceptor extends HandlerInterceptorAdapter {

	final MeterRegistry meterRegistry;

	public UserAgentMetricsInterceptor(MeterRegistry registry) {
		this.meterRegistry = registry;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
			Object handler) throws Exception {
		Map r = Classifier.parse(request.getHeader(HttpHeaders.USER_AGENT));
		String name = (String) r.get(DataSet.DATASET_KEY_NAME);
		String category = (String) r.get(DataSet.DATASET_KEY_CATEGORY);

		List<Tag> tags = new ArrayList<>();
		if (!StringUtils.isEmpty(name)) {
			tags.add(Tag.of("name", name));
		}
		if (!StringUtils.isEmpty(category)) {
			tags.add(Tag.of("category", category));
		}
		this.meterRegistry.counter("useragent", tags).increment();
		return true;
	}
}
