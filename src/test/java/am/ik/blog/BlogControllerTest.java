package am.ik.blog;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;

import am.ik.blog.entry.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.CacheControl;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;

import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;
import static java.util.concurrent.TimeUnit.HOURS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.HttpHeaders.LAST_MODIFIED;
import static org.springframework.http.HttpStatus.NOT_MODIFIED;
import static org.springframework.http.HttpStatus.OK;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class BlogControllerTest {
	@MockBean
	BlogClient blogClient;
	@Autowired
	WebTestClient webTestClient;

	@Test
	public void entries_ok_200() throws Exception {
		Entry entry1 = entry99999();
		Entry entry2 = entry99998();

		given(blogClient.streamAll(any())) //
				.willReturn(Flux.just(entry1, entry2));

		this.webTestClient.get() //
				.uri("/") //
				.exchange()//
				.expectStatus().isEqualTo(OK) //
				.expectHeader()
				.valueEquals(LAST_MODIFIED, entry1.getUpdated().getDate().getValue()
						.atZoneSameInstant(ZoneId.of("GMT")).format(RFC_1123_DATE_TIME)) //
				.expectHeader().cacheControl(CacheControl.maxAge(3, HOURS));
	}

	@Test
	public void entries_empty_200() throws Exception {
		Entry entry1 = entry99999();

		given(blogClient.streamAll(any())) //
				.willReturn(Flux.empty());

		ZonedDateTime dateTime = entry1.getUpdated().getDate().getValue()
				.atZoneSameInstant(ZoneId.of("GMT"));

		this.webTestClient.get() //
				.uri("/") //
				.ifModifiedSince(dateTime) //
				.exchange()//
				.expectStatus().isEqualTo(OK) //
				.expectHeader().doesNotExist(LAST_MODIFIED);
	}

	@Test
	public void entries_not_modified_304() throws Exception {
		Entry entry1 = entry99999();
		Entry entry2 = entry99998();

		given(blogClient.streamAll(any())) //
				.willReturn(Flux.just(entry1, entry2));

		ZonedDateTime dateTime = entry1.getUpdated().getDate().getValue()
				.atZoneSameInstant(ZoneId.of("GMT"));
		this.webTestClient.get() //
				.uri("/") //
				.ifModifiedSince(dateTime) //
				.exchange()//
				.expectStatus().isEqualTo(NOT_MODIFIED) //
				.expectHeader()
				.valueEquals(LAST_MODIFIED, dateTime.format(RFC_1123_DATE_TIME)) //
				.expectHeader().cacheControl(CacheControl.maxAge(3, HOURS));
	}

	@Test
	public void entries_modified_200() throws Exception {
		Entry entry1 = entry99999();
		Entry entry2 = entry99998();

		given(blogClient.streamAll(any())) //
				.willReturn(Flux.just(entry1, entry2));

		ZonedDateTime dateTime = entry1.getUpdated().getDate().getValue()
				.atZoneSameInstant(ZoneId.of("GMT"));
		this.webTestClient.get() //
				.uri("/") //
				.ifModifiedSince(dateTime.minusHours(1)).exchange()//
				.expectStatus().isEqualTo(OK) //
				.expectHeader()
				.valueEquals(LAST_MODIFIED, dateTime.format(RFC_1123_DATE_TIME)) //
				.expectHeader().cacheControl(CacheControl.maxAge(3, HOURS));
	}

	@Test
	public void entry_ok_200() throws Exception {
		Entry entry = entry99999();

		given(blogClient.findById(100L)) //
				.willReturn(Mono.just(entry));

		this.webTestClient.get() //
				.uri("/entries/100") //
				.exchange()//
				.expectStatus().isEqualTo(OK) //
				.expectHeader()
				.valueEquals(LAST_MODIFIED, entry.getUpdated().getDate().getValue()
						.atZoneSameInstant(ZoneId.of("GMT")).format(RFC_1123_DATE_TIME)) //
				.expectHeader().cacheControl(CacheControl.maxAge(3, HOURS));
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
				.ifModifiedSince(dateTime) //
				.exchange()//
				.expectStatus().isEqualTo(NOT_MODIFIED) //
				.expectHeader()
				.valueEquals(LAST_MODIFIED, dateTime.format(RFC_1123_DATE_TIME)) //
				.expectHeader().cacheControl(CacheControl.maxAge(3, HOURS));
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
				.expectStatus().isEqualTo(OK) //
				.expectHeader()
				.valueEquals(LAST_MODIFIED, dateTime.format(RFC_1123_DATE_TIME)) //
				.expectHeader().cacheControl(CacheControl.maxAge(3, HOURS));
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

	public static Entry entry99998() {
		return Entry.builder().entryId(new EntryId(99998L))
				.created(new Author(new Name("test"), EventTime.UNSET))
				.updated(new Author(new Name("test"), EventTime.UNSET))
				.frontMatter(new FrontMatter(new Title("sample"),
						new Categories(Arrays.asList(new Category("category"))),
						new Tags(Arrays.asList(new Tag("sample"))),
						new EventTime(OffsetDateTime.of(2017, 3, 1, 1, 0, 0, 0,
								ZoneOffset.ofHours(9))),
						new EventTime(OffsetDateTime.of(2017, 2, 1, 1, 0, 0, 0,
								ZoneOffset.ofHours(9))),
						new PremiumPoint(100)))
				.content(new Content("sample data!")).build().useFrontMatterDate();
	}
}