package am.ik.blog.renderer;

import java.util.List;
import java.util.stream.Collectors;

import am.ik.blog.model.Tag;

import org.springframework.stereotype.Component;

import static java.lang.String.format;

@Component
public class TagsRenderer {
	public String render(List<Tag> tags) {
		return tags.stream() //
				.map(this::render) //
				.collect(Collectors.joining("&nbsp;"));
	}

	public String render(Tag tag) {
		return format("\uD83C\uDFF7&nbsp;<a href=\"/tags/%s/entries\">%s</a>",
				tag.getName(), tag.getName());
	}
}
