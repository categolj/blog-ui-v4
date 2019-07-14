package am.ik.blog;

import java.util.List;

import am.ik.blog.model.Entry;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class BlogEntries {
	private final List<Entry> content;

	@JsonCreator
	public BlogEntries(@JsonProperty("content") List<Entry> content) {
		this.content = content;
	}

	public List<Entry> getContent() {
		return content;
	}
}
