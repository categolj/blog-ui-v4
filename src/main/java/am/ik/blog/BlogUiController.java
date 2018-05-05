package am.ik.blog;

import java.util.List;

import am.ik.blog.entry.*;
import org.thymeleaf.spring5.context.webflux.ReactiveDataDriverContextVariable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.result.view.Rendering;

@Controller
public class BlogUiController {
	private final BlogClient blogClient;

	public BlogUiController(BlogClient blogClient) {
		this.blogClient = blogClient;
	}

	@GetMapping({ "/", "/entries" })
	public Rendering home(@PageableDefault(size = 50) Pageable pageable) {
		Flux<Entry> entries = this.blogClient.streamAll(pageable);
		return Rendering.view("index") //
				.modelAttribute("entries", new ReactiveDataDriverContextVariable(entries)) //
				.build();
	}

	@GetMapping(path = { "/", "/entries" }, params = "q")
	public Rendering search(@RequestParam("q") String query,
			@PageableDefault(size = 50) Pageable pageable) {
		Flux<Entry> entries = this.blogClient.streamByQuery(query, pageable);
		return Rendering.view("index") //
				.modelAttribute("entries", new ReactiveDataDriverContextVariable(entries)) //
				.modelAttribute("query", query) //
				.build();
	}

	@GetMapping("/tags/{tag}/entries")
	public Rendering byTag(@PathVariable("tag") Tag tag,
			@PageableDefault(size = 100) Pageable pageable) {
		Flux<Entry> entries = this.blogClient.streamByTag(tag, pageable);
		return Rendering.view("index") //
				.modelAttribute("entries", new ReactiveDataDriverContextVariable(entries)) //
				.modelAttribute("tag", tag) //
				.build();
	}

	@GetMapping("/categories/{categories}/entries")
	public Rendering byCategories(@PathVariable("categories") List<Category> categories,
			@PageableDefault(size = 100) Pageable pageable) {
		Flux<Entry> entries = this.blogClient.streamByCategories(categories, pageable);
		return Rendering.view("index") //
				.modelAttribute("entries", new ReactiveDataDriverContextVariable(entries)) //
				.modelAttribute("categories", new Categories(categories)) //
				.build();
	}

	@GetMapping("/entries/{entryId}")
	public Rendering byId(@PathVariable("entryId") Long entryId) {
		Mono<Entry> entry = this.blogClient.findById(entryId).cache();
		return Rendering.view("entry") //
				.modelAttribute("entry", entry) //
				.modelAttribute("checker", entry.map(ContentChecker::new)) //
				.build();
	}

	@GetMapping("/p/entries/{entryId}")
	public Rendering premiumById(@PathVariable("entryId") Long entryId) {
		return Rendering.view("redirect:/entries/{entryId}").build();
	}

	@GetMapping("/categories")
	public Rendering categories() {
		Flux<Categories> categories = this.blogClient.streamCategories();
		return Rendering.view("categories") //
				.modelAttribute("categories",
						new ReactiveDataDriverContextVariable(categories)) //
				.build();
	}

	@GetMapping("/tags")
	public Rendering tags() {
		Flux<Tag> tags = this.blogClient.streamTags();
		return Rendering.view("tags") //
				.modelAttribute("tags", new ReactiveDataDriverContextVariable(tags)) //
				.build();
	}

	public static class ContentChecker {
		private final EventTime created;
		private Boolean isQuiteDanger;
		private Boolean isDanger;
		private Boolean isWarning;
		private Boolean isCaution;

		public ContentChecker(Entry entry) {
			this.created = entry.getUpdated().getDate();
			this.isQuiteDanger = this.isQuiteDanger();
			this.isDanger = this.isDanger();
			this.isWarning = this.isWarning();
			this.isCaution = this.isCaution();
		}

		public boolean isQuiteDanger() {
			if (this.isQuiteDanger != null) {
				return this.isQuiteDanger;
			}
			return this.created.isOverFiveYearsOld();
		}

		public boolean isDanger() {
			if (this.isDanger != null) {
				return this.isDanger;
			}
			return !this.isQuiteDanger() && this.created.isOverThreeYearsOld();
		}

		public boolean isWarning() {
			if (this.isWarning != null) {
				return this.isWarning;
			}
			return !this.isQuiteDanger() && !this.isDanger()
					&& this.created.isOverOneYearOld();
		}

		public boolean isCaution() {
			if (this.isCaution != null) {
				return this.isCaution;
			}
			return !this.isQuiteDanger() && !this.isDanger() && !this.isWarning()
					&& this.created.isOverHalfYearOld();
		}

	}
}
