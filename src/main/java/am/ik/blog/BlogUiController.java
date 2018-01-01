package am.ik.blog;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import am.ik.blog.entry.Categories;
import am.ik.blog.entry.Category;
import am.ik.blog.entry.Tag;
import reactor.core.publisher.Mono;

@Controller
public class BlogUiController {
	private final BlogClient blogClient;

	public BlogUiController(BlogClient blogClient) {
		this.blogClient = blogClient;
	}

	@GetMapping({ "/", "/entries" })
	public Mono<ModelAndView> home(@PageableDefault(size = 50) Pageable pageable) {
		return this.blogClient.findAll(pageable) //
				.map(entries -> new ModelAndView("index") //
						.addObject("entries", entries));
	}

	@GetMapping(path = { "/", "/entries" }, params = "q")
	public Mono<ModelAndView> search(@RequestParam("q") String query,
			@PageableDefault(size = 50) Pageable pageable) {
		return this.blogClient.findByQuery(query, pageable) //
				.map(entries -> new ModelAndView("index") //
						.addObject("query", query) //
						.addObject("entries", entries));
	}

	@GetMapping("/tags/{tag}/entries")
	public Mono<ModelAndView> byTag(@PathVariable("tag") Tag tag,
			@PageableDefault(size = 100) Pageable pageable) {
		return this.blogClient.findByTag(tag, pageable) //
				.map(entries -> new ModelAndView("index") //
						.addObject("tag", tag) //
						.addObject("entries", entries));
	}

	@GetMapping("/categories/{categories}/entries")
	public Mono<ModelAndView> byCategories(
			@PathVariable("categories") List<Category> categories,
			@PageableDefault(size = 100) Pageable pageable) {
		return this.blogClient.findByCategories(categories, pageable) //
				.map(entries -> new ModelAndView("index") //
						.addObject("categories", new Categories(categories)) //
						.addObject("entries", entries));
	}

	@GetMapping("/entries/{entryId}")
	public Mono<ModelAndView> byId(@PathVariable("entryId") Long entryId) {
		return this.blogClient.findById(entryId) //
				.map(entry -> new ModelAndView("entry") //
						.addObject("entry", entry));
	}

	@GetMapping("/p/entries/{entryId}")
	public ModelAndView premiumById(@PathVariable("entryId") Long entryId) {
		return new ModelAndView("premium") //
				.addObject("entryId", entryId);

	}

	@GetMapping("/categories")
	public Mono<ModelAndView> categories() {
		return this.blogClient.findCategories() //
				.map(categories -> new ModelAndView("categories") //
						.addObject("categories", categories));
	}

	@GetMapping("/tags")
	public Mono<ModelAndView> tags() {
		return this.blogClient.findTags() //
				.map(tags -> new ModelAndView("tags") //
						.addObject("tags", tags));
	}
}
