package am.ik.blog.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import am.ik.marked4j.Marked;
import am.ik.marked4j.MarkedBuilder;

@Configuration
public class BlogConfig {
	@Bean
	public Marked marked() {
		return new MarkedBuilder() //
				.breaks(true) //
				.autoToc(true) //
				.enableHeadingIdUriEncoding(true) //
				.build();
	}
}
