package am.ik.blog.renderer;

import static java.lang.String.format;

import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import am.ik.blog.entry.Tag;
import am.ik.blog.entry.Tags;

@Component
public class TagsRenderer {
	public String render(Tags tags) {
		ServletUriComponentsBuilder builder = ServletUriComponentsBuilder
				.fromCurrentContextPath();
		return tags.stream() //
				.map(tag -> this.render(tag, builder)) //
				.collect(Collectors.joining("&nbsp;"));
	}

	public String render(Tag tag) {
		ServletUriComponentsBuilder builder = ServletUriComponentsBuilder
				.fromCurrentContextPath();
		return this.render(tag, builder);
	}

	private String render(Tag tag, ServletUriComponentsBuilder builder) {
		return format("\uD83C\uDFF7&nbsp;<a href=\"%s\">%s</a>",
				builder.replacePath("/tags/{tag}/entries").buildAndExpand(tag), tag);
	}
}
