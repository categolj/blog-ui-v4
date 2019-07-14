package am.ik.blog.model;

import java.util.List;

import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

@JsonPOJOBuilder
public class FrontMatterBuilder {

	private List<Category> categories;

	private List<Tag> tags;

	private String title;

	public FrontMatter build() {
		return new FrontMatter(title, categories, tags);
	}

	public FrontMatterBuilder withCategories(List<Category> categories) {
		this.categories = categories;
		return this;
	}

	public FrontMatterBuilder withTags(List<Tag> tags) {
		this.tags = tags;
		return this;
	}

	public FrontMatterBuilder withTitle(String title) {
		this.title = title;
		return this;
	}
}