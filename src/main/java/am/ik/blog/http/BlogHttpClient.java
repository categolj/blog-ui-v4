package am.ik.blog.http;

import java.util.List;

import am.ik.blog.BlogClient;
import am.ik.blog.BlogEntries;
import am.ik.blog.entry.Categories;
import am.ik.blog.entry.Category;
import am.ik.blog.entry.Entry;
import am.ik.blog.entry.Tag;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import static rx.RxReactiveStreams.toPublisher;

@Component
@ConditionalOnProperty(name = "blog.client.type", havingValue = "http", matchIfMissing = true)
public class BlogHttpClient implements BlogClient {
	private final RxBlogHttpClient rxBlogHttpClient;

	public BlogHttpClient(RxBlogHttpClient rxBlogHttpClient) {
		this.rxBlogHttpClient = rxBlogHttpClient;
	}

	@Override
	public Mono<Entry> findById(Long entryId) {
		return Mono.from(toPublisher(this.rxBlogHttpClient.findById(entryId)));
	}

	@Override
	public Mono<BlogEntries> findAll(Pageable pageable) {
		return Mono.from(toPublisher(this.rxBlogHttpClient.findAll(pageable)));
	}

	@Override
	public Flux<Entry> streamAll(Pageable pageable) {
		return Flux.from(toPublisher(this.rxBlogHttpClient.streamAll(pageable)));
	}

	@Override
	public Mono<BlogEntries> findByQuery(String query, Pageable pageable) {
		return Mono.from(toPublisher(this.rxBlogHttpClient.findByQuery(query, pageable)));
	}

	@Override
	public Mono<BlogEntries> findByCategories(List<Category> categories,
			Pageable pageable) {
		return Mono.from(toPublisher(
				this.rxBlogHttpClient.findByCategories(categories, pageable)));
	}

	@Override
	public Mono<BlogEntries> findByTag(Tag tag, Pageable pageable) {
		return Mono.from(toPublisher(this.rxBlogHttpClient.findByTag(tag, pageable)));
	}

	@Override
	public Mono<List<Tag>> findTags() {
		return Mono.from(toPublisher(this.rxBlogHttpClient.findTags()));
	}

	@Override
	public Mono<List<Categories>> findCategories() {
		return Mono.from(toPublisher(this.rxBlogHttpClient.findCategories()));
	}
}
