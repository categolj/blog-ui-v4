package am.ik.blog.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@ConfigurationProperties("security")
@Order(-5)
public class ActuatorConfig {
	private User user = new User();

	@Bean
	public SecurityWebFilterChain springWebFilterChain(ServerHttpSecurity http)
			throws Exception {
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

	@Bean
	public MapReactiveUserDetailsService reactiveUserDetailsService() {
		UserDetails user = org.springframework.security.core.userdetails.User
				.withDefaultPasswordEncoder() //
				.username(this.user.name) //
				.password(this.user.password) //
				.roles("ACTUATOR") //
				.build();
		return new MapReactiveUserDetailsService(user);
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public static class User {
		private String name = "user";
		private String password = "password";

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getPassword() {
			return password;
		}

		public void setPassword(String password) {
			this.password = password;
		}
	}
}
