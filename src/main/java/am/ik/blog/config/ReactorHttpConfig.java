package am.ik.blog.config;

import am.ik.blog.BlogProperties;
import reactor.netty.http.client.HttpClient;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;

import static io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS;

@Configuration
public class ReactorHttpConfig {
	@Bean
	public ClientHttpConnector clientHttpConnector(BlogProperties props) {
		HttpClient httpClient = HttpClient.create() //
				.wiretap(props.isDebugHttp()) //
				.tcpConfiguration(
						tcpClient -> tcpClient.option(CONNECT_TIMEOUT_MILLIS, 1_000));
		return new ReactorClientHttpConnector(httpClient);
	}
}
