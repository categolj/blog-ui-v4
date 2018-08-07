package am.ik.blog.http;

import java.time.Duration;
import java.util.List;

import am.ik.blog.BlogClient;
import am.ik.blog.BlogEntries;
import am.ik.blog.BlogProperties;
import am.ik.blog.entry.Categories;
import am.ik.blog.entry.Category;
import am.ik.blog.entry.Entry;
import am.ik.blog.entry.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.retry.Backoff;
import reactor.retry.Retry;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import static rx.RxReactiveStreams.toPublisher;

@Component
@ConditionalOnProperty(name = "blog.client.type", havingValue = "http", matchIfMissing = true)
public class BlogHttpClient implements BlogClient {
	private static final Logger log = LoggerFactory.getLogger(BlogHttpClient.class);

	private final RxBlogHttpClient rxBlogHttpClient;
	private final Retry<String> retry;

	public BlogHttpClient(RxBlogHttpClient rxBlogHttpClient, BlogProperties props) {
		this.rxBlogHttpClient = rxBlogHttpClient;
		this.retry = Retry.<String>onlyIf(ctx -> {
			Throwable exception = ctx.exception();
			if (ResponseStatusException.class.isInstance(exception)) {
				// Do not retry for client error
				ResponseStatusException e = ResponseStatusException.class.cast(exception);
				return !e.getStatus().is4xxClientError();
			}
			return true;
		}) //
                .timeout(Duration.ofSeconds(6))
				.retryMax(props.getRetryMax()) //
				.backoff(Backoff.exponential(props.getRetryFirstBackoff(),
						Duration.ofSeconds(10), 2, false)) //
				.doOnRetry(ctx -> log.warn("Retrying {}({})", ctx.applicationContext(),
						ctx));
	}

	@Override
	public Mono<Entry> findById(Long entryId) {
		return Mono.defer(
				() -> Mono.from(toPublisher(this.rxBlogHttpClient.findById(entryId))))
				.retryWhen(this.retry.withApplicationContext("findById(Long)"));
	}

	@Override
	public Mono<BlogEntries> findAll(Pageable pageable) {
		return Mono.defer(() -> Mono
				.from(toPublisher(this.rxBlogHttpClient.findAll(pageable)))
				.retryWhen(this.retry.withApplicationContext("findAll(Pageable)")));
	}

	@Override
	public Flux<Entry> streamAll(Pageable pageable) {
		return Flux.defer(
				() -> Flux.from(toPublisher(this.rxBlogHttpClient.streamAll(pageable))))
				.retryWhen(this.retry.withApplicationContext("streamAll(Pageable)"));
	}

	@Override
	public Mono<BlogEntries> findByQuery(String query, Pageable pageable) {
		return Mono
				.defer(() -> Mono.from(
						toPublisher(this.rxBlogHttpClient.findByQuery(query, pageable))))
				.retryWhen(this.retry
						.withApplicationContext("findByQuery(String, Pageable)"));
	}

	@Override
	public Mono<BlogEntries> findByCategories(List<Category> categories,
			Pageable pageable) {
		return Mono
				.defer(() -> Mono.from(toPublisher(
						this.rxBlogHttpClient.findByCategories(categories, pageable))))
				.retryWhen(this.retry.withApplicationContext(
						"findByCategories(List<Category>, Pageable)"));
	}

	@Override
	public Mono<BlogEntries> findByTag(Tag tag, Pageable pageable) {
		return Mono
				.defer(() -> Mono.from(
						toPublisher(this.rxBlogHttpClient.findByTag(tag, pageable))))
				.retryWhen(this.retry.withApplicationContext("findByTag(Tag, Pageable)"));
	}

	@Override
	public Mono<List<Tag>> findTags() {
		return Mono.defer(() -> Mono.from(toPublisher(this.rxBlogHttpClient.findTags())))
				.retryWhen(this.retry.withApplicationContext("findTags()"));
	}

	@Override
	public Mono<List<Categories>> findCategories() {
		return Mono.defer(
				() -> Mono.from(toPublisher(this.rxBlogHttpClient.findCategories())))
				.retryWhen(this.retry.withApplicationContext("findCategories()"));
	}
}
