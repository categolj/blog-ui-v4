package am.ik.blog;

import java.util.List;

import am.ik.blog.entry.Categories;
import am.ik.blog.entry.Category;
import am.ik.blog.entry.Entry;
import am.ik.blog.entry.Tag;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.data.domain.Pageable;

public interface BlogClient {

	Mono<Entry> findById(Long entryId);

	Mono<BlogEntries> findAll(Pageable pageable);

	default Flux<Entry> streamAll(Pageable pageable) {
		return this.findAll(pageable).flatMapMany(x -> Flux.fromIterable(x.getContent()));
	}

	Mono<BlogEntries> findByQuery(String query, Pageable pageable);

	default Flux<Entry> streamByQuery(String query, Pageable pageable) {
		return this.findByQuery(query, pageable)
				.flatMapMany(x -> Flux.fromIterable(x.getContent()));
	}

	Mono<BlogEntries> findByCategories(List<Category> categories, Pageable pageable);

	default Flux<Entry> streamByCategories(List<Category> categories, Pageable pageable) {
		return this.findByCategories(categories, pageable)
				.flatMapMany(x -> Flux.fromIterable(x.getContent()));
	}

	Mono<BlogEntries> findByTag(Tag tag, Pageable pageable);

	default Flux<Entry> streamByTag(Tag tag, Pageable pageable) {
		return this.findByTag(tag, pageable)
				.flatMapMany(x -> Flux.fromIterable(x.getContent()));
	}

	Mono<List<Tag>> findTags();

	default Flux<Tag> streamTags() {
		return this.findTags().flatMapMany(Flux::fromIterable);
	}

	Mono<List<Categories>> findCategories();

	default Flux<Categories> streamCategories() {
		return this.findCategories().flatMapMany(Flux::fromIterable);
	}
}
