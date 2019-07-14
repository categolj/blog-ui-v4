package am.ik.blog.model;

import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

@JsonPOJOBuilder
public class EntryBuilder {

	private String content;

	private Author created;

	private Long entryId;

	private FrontMatter frontMatter;

	private Author updated;

	public Entry build() {
		return new Entry(entryId, frontMatter, content, created, updated);
	}

	public EntryBuilder withContent(String content) {
		this.content = content;
		return this;
	}

	public EntryBuilder withCreated(Author created) {
		this.created = created;
		return this;
	}

	public EntryBuilder withEntryId(Long entryId) {
		this.entryId = entryId;
		return this;
	}

	public EntryBuilder withFrontMatter(FrontMatter frontMatter) {
		this.frontMatter = frontMatter;
		return this;
	}

	public EntryBuilder withUpdated(Author updated) {
		this.updated = updated;
		return this;
	}
}