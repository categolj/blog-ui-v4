package am.ik.blog.model;

import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Category {
	private final String name;

	@JsonCreator
	public static Category of(@JsonProperty("name") String name) {
		return new Category(name);
	}

	private Category(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return new StringJoiner(", ", Category.class.getSimpleName() + "[", "]")
				.add("name='" + name + "'").toString();
	}
}
