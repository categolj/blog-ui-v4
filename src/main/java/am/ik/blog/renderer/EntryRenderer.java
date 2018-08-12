package am.ik.blog.renderer;

import am.ik.blog.entry.Entry;
import am.ik.marked4j.Marked;

import org.springframework.cloud.sleuth.annotation.NewSpan;
import org.springframework.stereotype.Component;

@Component
public class EntryRenderer {
	private final Marked marked;

	public EntryRenderer(Marked marked) {
		this.marked = marked;
	}

	@NewSpan
	public String render(Entry entry) {
		return this.marked.marked(entry.content().getValue());
	}
}
