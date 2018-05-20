package am.ik.blog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.hystrix.EnableHystrix;

@SpringBootApplication
@EnableHystrix
public class BlogUiApplication {

	public static void main(String[] args) {
		SpringApplication.run(BlogUiApplication.class, args);
	}
}
