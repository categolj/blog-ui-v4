package am.ik.blog.model;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;

@JsonDeserialize(builder = AuthorBuilder.class)
public class Author {
	private final String name;
	private final OffsetDateTime date;

	Author(String name, OffsetDateTime date) {
		this.name = name;
		this.date = date;
	}

	public String getName() {
		return name;
	}

	public OffsetDateTime getDate() {
		return date;
	}

	public String rfc1123DateTime() {
		if (this.date == null) {
			return "";
		}
		return this.date.format(RFC_1123_DATE_TIME);
	}
}
