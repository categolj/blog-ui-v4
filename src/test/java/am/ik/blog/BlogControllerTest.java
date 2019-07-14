package am.ik.blog;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;

import am.ik.blog.model.AuthorBuilder;
import am.ik.blog.model.Category;
import am.ik.blog.model.Entry;
import am.ik.blog.model.EntryBuilder;
import am.ik.blog.model.FrontMatterBuilder;
import am.ik.blog.model.Tag;
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
				.valueEquals(LAST_MODIFIED,
						entry1.getUpdated().getDate().atZoneSameInstant(ZoneId.of("GMT"))
								.format(RFC_1123_DATE_TIME)) //
				.expectHeader().cacheControl(CacheControl.maxAge(3, HOURS));
	}

	@Test
	public void entries_empty_200() throws Exception {
		Entry entry1 = entry99999();

		given(blogClient.streamAll(any())) //
				.willReturn(Flux.empty());

		ZonedDateTime dateTime = entry1.getUpdated().getDate()
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

		ZonedDateTime dateTime = entry1.getUpdated().getDate()
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

		ZonedDateTime dateTime = entry1.getUpdated().getDate()
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
				.valueEquals(LAST_MODIFIED,
						entry.getUpdated().getDate().atZoneSameInstant(ZoneId.of("GMT"))
								.format(RFC_1123_DATE_TIME)) //
				.expectHeader().cacheControl(CacheControl.maxAge(3, HOURS));
	}

	@Test
	public void entry_not_modified_304() throws Exception {
		Entry entry = entry99999();

		given(blogClient.findById(100L)) //
				.willReturn(Mono.just(entry));

		ZonedDateTime dateTime = entry.getUpdated().getDate()
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

		ZonedDateTime dateTime = entry.getUpdated().getDate()
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
		return new EntryBuilder() //
				.withEntryId(99999L) //
				.withContent("This is a test data.") //
				.withCreated(new AuthorBuilder() //
						.withName("making") //
						.withDate(OffsetDateTime.parse("2017-04-01T01:00:00+09:00")) //
						.build()) //
				.withUpdated(new AuthorBuilder() //
						.withName("making") //
						.withDate(OffsetDateTime.parse("2017-04-01T02:00:00+09:00")) //
						.build()) //
				.withFrontMatter(new FrontMatterBuilder() //
						.withTitle("Hello World!!") //
						.withTags(Arrays.asList(Tag.of("test1"), Tag.of("test2"),
								Tag.of("test3"))) //
						.withCategories(Arrays.asList(Category.of("x"), Category.of("y"),
								Category.of("z"))) //
						.build()) //
				.build();
	}

	public static Entry entry99998() {
		return new EntryBuilder() //
				.withEntryId(99998L) //
				.withContent("sample data!") //
				.withCreated(new AuthorBuilder() //
						.withName("test") //
						.withDate(OffsetDateTime.parse("2017-03-01T01:00:00+09:00")) //
						.build()) //
				.withUpdated(new AuthorBuilder() //
						.withName("test") //
						.withDate(OffsetDateTime.parse("2017-02-01T01:00:00+09:00")) //
						.build()) //
				.withFrontMatter(new FrontMatterBuilder() //
						.withTitle("sample") //
						.withTags(Arrays.asList(Tag.of("sample"))) //
						.withCategories(Arrays.asList(Category.of("category"))) //
						.build()) //
				.build();
	}
}