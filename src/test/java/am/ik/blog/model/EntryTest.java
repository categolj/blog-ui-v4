package am.ik.blog.model;

import java.time.OffsetDateTime;
import java.util.Arrays;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import org.junit.Test;

import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import static org.assertj.core.api.Assertions.assertThat;

public class EntryTest {
	ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json()
			.dateFormat(new StdDateFormat()) //
			.build();

	final String testJson = "{\"entryId\":99999,\"frontMatter\":{\"title\":\"Hello World!!\",\"categories\":[{\"name\":\"x\"},{\"name\":\"y\"},{\"name\":\"z\"}],"
			+ "\"tags\":[{\"name\":\"test1\"},{\"name\":\"test2\"},{\"name\":\"test3\"}]},\"content\":\"This is a test data.\",\"created\":{\"name\":\"making\",\"date\":\"2017-04-01T01:00:00+09:00\"},"
			+ "\"updated\":{\"name\":\"making\",\"date\":\"2017-04-01T02:00:00+09:00\"}}";

	@Test
	public void serialize() throws Exception {
		final Entry entry = new EntryBuilder() //
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
		final String json = objectMapper.writeValueAsString(entry);

		assertThat(json).isEqualTo(testJson);
	}

	@Test
	public void deserialize() throws Exception {
		final Entry entry = this.objectMapper.readValue(testJson, Entry.class);
		assertThat(entry.getEntryId()).isEqualTo(99999L);
		assertThat(entry.getContent()).isEqualTo("This is a test data.");
		assertThat(entry.getFrontMatter()).isNotNull();
		assertThat(entry.getFrontMatter().getTitle()).isEqualTo("Hello World!!");
		assertThat(entry.getFrontMatter().getTags()).hasSize(3);
		assertThat(entry.getFrontMatter().getTags().get(0).getName()).isEqualTo("test1");
		assertThat(entry.getFrontMatter().getTags().get(1).getName()).isEqualTo("test2");
		assertThat(entry.getFrontMatter().getTags().get(2).getName()).isEqualTo("test3");
		assertThat(entry.getFrontMatter().getCategories()).hasSize(3);
		assertThat(entry.getFrontMatter().getCategories().get(0).getName())
				.isEqualTo("x");
		assertThat(entry.getFrontMatter().getCategories().get(1).getName())
				.isEqualTo("y");
		assertThat(entry.getFrontMatter().getCategories().get(2).getName())
				.isEqualTo("z");
		assertThat(entry.getCreated()).isNotNull();
		assertThat(entry.getCreated().getName()).isEqualTo("making");
		assertThat(entry.getCreated().getDate().toInstant())
				.isEqualTo(OffsetDateTime.parse("2017-04-01T01:00:00+09:00").toInstant());
		assertThat(entry.getUpdated()).isNotNull();
		assertThat(entry.getUpdated().getName()).isEqualTo("making");
		assertThat(entry.getUpdated().getDate().toInstant())
				.isEqualTo(OffsetDateTime.parse("2017-04-01T02:00:00+09:00").toInstant());
	}
}