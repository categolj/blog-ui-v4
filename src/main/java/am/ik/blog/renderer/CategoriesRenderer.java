package am.ik.blog.renderer;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import am.ik.blog.entry.Categories;
import am.ik.blog.entry.Category;

@Component
public class CategoriesRenderer {
	public String render(Categories categories) {
		List<String> list = categories.stream().map(Category::toString).collect(toList());
		if (list == null || list.isEmpty()) {
			return "";
		}
		List<String> ret = new ArrayList<>(list.size());
		List<String> buf = new ArrayList<>(list.size());
		list.forEach(category -> {
			buf.add(category);
			ret.add(format("<a href=\"/categories/%s/entries\">%s</a>",
					buf.stream().collect(joining(",")), category));
		});
		return "\uD83D\uDDC3&nbsp;{" + ret.stream().collect(joining("/")) + "}";
	}
}
