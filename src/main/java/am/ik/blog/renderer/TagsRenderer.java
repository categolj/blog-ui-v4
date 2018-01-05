package am.ik.blog.renderer;

import static java.lang.String.format;

import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import am.ik.blog.entry.Tag;
import am.ik.blog.entry.Tags;

@Component
public class TagsRenderer {
	public String render(Tags tags) {
		return tags.stream() //
				.map(this::render) //
				.collect(Collectors.joining("&nbsp;"));
	}

	public String render(Tag tag) {
		return format("\uD83C\uDFF7&nbsp;<a href=\"/tags/%s/entries\">%s</a>", tag, tag);
	}
}
