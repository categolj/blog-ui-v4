package am.ik.blog.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
public class ActuatorConfig {

	@Bean
	public SecurityWebFilterChain springWebFilterChain(ServerHttpSecurity http) {
		return http //
				.httpBasic() //
				.and() //
				.authorizeExchange() //
				.pathMatchers("/actuator/health", "/actuator/info").permitAll() //
				.pathMatchers("/actuator/**").hasRole("ACTUATOR") //
				.anyExchange().permitAll() //
				.and() //
				.build();
	}
}
