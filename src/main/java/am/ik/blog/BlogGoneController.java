package am.ik.blog;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BlogGoneController {
	@GetMapping("/entry/view/**")
	@ResponseStatus(HttpStatus.GONE)
	public String gone() {
		return "RIP";
	}

	@GetMapping("/upload/**")
	@ResponseStatus(HttpStatus.GONE)
	public String upload() {
		return "RIP";
	}

	@GetMapping("/api/v1/**")
	@ResponseStatus(HttpStatus.GONE)
	public String apiV1() {
		return "RIP";
	}
}
