package am.ik.blog.config;

import am.ik.marked4j.Marked;
import am.ik.marked4j.MarkedBuilder;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.context.support.SimpleThreadScope;

@Configuration
public class BlogConfig {
	@Bean
	public static BeanFactoryPostProcessor beanFactoryPostProcessor() {
		return beanFactory -> beanFactory.registerScope("thread",
				new SimpleThreadScope());
	}

	@Bean
	@Scope(scopeName = "thread", proxyMode = ScopedProxyMode.TARGET_CLASS)
	public Marked marked() {
		return new MarkedBuilder() //
				.breaks(true) //
				.autoToc(true) //
				.enableHeadingIdUriEncoding(true) //
				.build();
	}
}
