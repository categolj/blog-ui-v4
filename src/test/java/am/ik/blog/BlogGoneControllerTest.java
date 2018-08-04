package am.ik.blog;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;

@RunWith(SpringRunner.class)
@WebFluxTest
public class BlogGoneControllerTest {
	@MockBean
	BlogClient blogClient;
	@Autowired
	WebTestClient webTestClient;

	@Test
	public void view() throws Exception {
		this.webTestClient.get() //
				.uri("/entry/view/id/100") //
				.exchange()//
				.expectStatus() //
				.isEqualTo(HttpStatus.GONE);
	}

	@Test
	public void upload() throws Exception {
		this.webTestClient.get() //
				.uri("/upload/00012/uploaded-3478149840329.sh") //
				.exchange()//
				.expectStatus() //
				.isEqualTo(HttpStatus.GONE);
	}

	@Test
	public void apiV1() throws Exception {
		this.webTestClient.get() //
				.uri("/api/v1/files/00108/r05.png") //
				.exchange()//
				.expectStatus() //
				.isEqualTo(HttpStatus.GONE);
	}

	@Test
	public void m() throws Exception {
		this.webTestClient.get() //
				.uri("/m/entry/view/id/191") //
				.exchange()//
				.expectStatus() //
				.isEqualTo(HttpStatus.GONE);
	}
}