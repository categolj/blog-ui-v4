package am.ik.blog.model;

import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Tag {
	private final String name;

	@JsonCreator
	public static Tag of(@JsonProperty("name") String name) {
		return new Tag(name);
	}

	private Tag(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return new StringJoiner(", ", Tag.class.getSimpleName() + "[", "]")
				.add("name='" + name + "'").toString();
	}
}
