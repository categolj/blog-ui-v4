package am.ik.blog;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import am.ik.blog.entry.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.mockito.BDDMockito.given;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class BlogControllerTest {
	@MockBean
	BlogClient blogClient;
	@Autowired
	WebTestClient webTestClient;

	@Test
	public void entry_ok_200() throws Exception {
		Entry entry = entry99999();

		given(blogClient.findById(100L)) //
				.willReturn(Mono.just(entry));

		this.webTestClient.get() //
				.uri("/entries/100") //
				.exchange()//
				.expectStatus().isEqualTo(HttpStatus.OK) //
				.expectHeader()
				.valueEquals(HttpHeaders.LAST_MODIFIED,
						entry.getUpdated().getDate().getValue()
								.atZoneSameInstant(ZoneId.of("GMT"))
								.format(DateTimeFormatter.RFC_1123_DATE_TIME)) //
				.expectHeader().cacheControl(CacheControl.maxAge(3, TimeUnit.HOURS));
	}

	@Test
	public void entry_not_modified_304() throws Exception {
		Entry entry = entry99999();

		given(blogClient.findById(100L)) //
				.willReturn(Mono.just(entry));

		ZonedDateTime dateTime = entry.getUpdated().getDate().getValue()
				.atZoneSameInstant(ZoneId.of("GMT"));
		this.webTestClient.get() //
				.uri("/entries/100") //
				.ifModifiedSince(dateTime).exchange()//
				.expectStatus().isEqualTo(HttpStatus.NOT_MODIFIED) //
				.expectHeader()
				.valueEquals(HttpHeaders.LAST_MODIFIED,
						dateTime.format(DateTimeFormatter.RFC_1123_DATE_TIME)) //
				.expectHeader().cacheControl(CacheControl.maxAge(3, TimeUnit.HOURS));
	}

	@Test
	public void entry_modified_200() throws Exception {
		Entry entry = entry99999();

		given(blogClient.findById(100L)) //
				.willReturn(Mono.just(entry));

		ZonedDateTime dateTime = entry.getUpdated().getDate().getValue()
				.atZoneSameInstant(ZoneId.of("GMT"));
		this.webTestClient.get() //
				.uri("/entries/100") //
				.ifModifiedSince(dateTime.minusHours(1)).exchange()//
				.expectStatus().isEqualTo(HttpStatus.OK) //
				.expectHeader()
				.valueEquals(HttpHeaders.LAST_MODIFIED,
						dateTime.format(DateTimeFormatter.RFC_1123_DATE_TIME)) //
				.expectHeader().cacheControl(CacheControl.maxAge(3, TimeUnit.HOURS));
	}

	public static Entry entry99999() {
		return Entry.builder().entryId(new EntryId(99999L))
				.created(new Author(new Name("test"), EventTime.UNSET))
				.updated(new Author(new Name("test"), EventTime.UNSET))
				.frontMatter(new FrontMatter(new Title("test"),
						new Categories(Arrays.asList(new Category("category"))),
						new Tags(Arrays.asList(new Tag("tag"))),
						new EventTime(OffsetDateTime.of(2017, 4, 1, 1, 0, 0, 0,
								ZoneOffset.ofHours(9))),
						new EventTime(OffsetDateTime.of(2017, 5, 1, 1, 0, 0, 0,
								ZoneOffset.ofHours(9))),
						new PremiumPoint(100)))
				.content(new Content("test data!")).build().useFrontMatterDate();
	}
}