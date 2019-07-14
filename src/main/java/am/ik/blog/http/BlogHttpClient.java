package am.ik.blog.http;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import am.ik.blog.BlogClient;
import am.ik.blog.BlogEntries;
import am.ik.blog.BlogProperties;
import am.ik.blog.model.Category;
import am.ik.blog.model.Entry;
import am.ik.blog.model.Tag;
import brave.Tracer;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.cache.CacheMono;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Signal;

import org.springframework.cloud.circuitbreaker.commons.ReactiveCircuitBreakerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import static java.util.stream.Collectors.joining;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.HttpHeaders.IF_MODIFIED_SINCE;
import static org.springframework.http.HttpStatus.NOT_MODIFIED;

@Component
public class BlogHttpClient implements BlogClient {
	private final WebClient webClient;
	private final Cache<Long, Entry> entryCache;
	private final Decorator decorator;
	private final ReactiveCircuitBreakerFactory circuitBreakerFactory;
	private final Retryer retryer;
	private static final Logger log = LoggerFactory.getLogger(BlogHttpClient.class);

	public BlogHttpClient(WebClient.Builder builder, MeterRegistry meterRegistry,
			BlogProperties props, ReactiveCircuitBreakerFactory circuitBreakerFactory,
			Tracer tracer) {
		this.entryCache = CaffeineCacheMetrics.monitor(meterRegistry, Caffeine
				.newBuilder() //
				.maximumSize(100) //
				.expireAfterWrite(3, TimeUnit.DAYS) //
				.removalListener(
						(key, value, cause) -> log.info("Remove cache(entryId={})", key)) //
				.build(), "entryCache");
		this.circuitBreakerFactory = circuitBreakerFactory;
		this.retryer = new Retryer(tracer, props.getRetry());
		this.decorator = new Decorator(this.retryer, circuitBreakerFactory);
		this.webClient = builder.baseUrl(props.getApi().getUrl()) //
				.build();
	}

	public Mono<Entry> findById(Long entryId) {
		Mono<Entry> entry = CacheMono
				.lookup(key -> Mono.justOrEmpty(this.entryCache.get(key, x -> null)) //
						.flatMap(e -> this.webClient.head() //
								.uri("entries/{entryId}", entryId) //
								.header(IF_MODIFIED_SINCE,
										e.getUpdated().rfc1123DateTime()) //
								.exchange() //
								.filter(r -> r.statusCode() == NOT_MODIFIED) //
								.map(x -> e))
						.map(Signal::next), entryId) //
				.onCacheMissResume(() -> this.webClient.get() //
						.uri("entries/{entryId}?excludeContent=false", entryId) //
						.retrieve() //
						.bodyToMono(Entry.class)) //
				.andWriteWith((key, signal) -> Mono.justOrEmpty(signal.get())
						.doOnNext(e -> this.entryCache.put(key, e)).then());

		Function<Throwable, Mono<Entry>> fallback = error -> {
			Entry cached = this.entryCache.getIfPresent(entryId);
			return Mono.justOrEmpty(cached) //
					.switchIfEmpty(Mono.error(error));
		};

		final String name = "blog-ui.findById";
		return entry.transform(this.retryer.retry(name))
				.transform(x -> this.circuitBreakerFactory.create(name).run(x, fallback));
	}

	public Mono<BlogEntries> findAll(Pageable pageable) {
		Mono<BlogEntries> entries = this.webClient.get() //
				.uri("entries?page={page}&size={size}&excludeContent=true",
						pageable.getPageNumber(), pageable.getPageSize()) //
				.retrieve() //
				.bodyToMono(BlogEntries.class);
		return entries.transform(this.decorator.decorate("blog-ui.findAll"));
	}

	public Flux<Entry> streamAll(Pageable pageable) {
		Flux<Entry> entries = this.webClient.get()
				.uri("entries?page={page}&size={size}&excludeContent=true",
						pageable.getPageNumber(), pageable.getPageSize()) //
				.header(ACCEPT, "application/stream+json").retrieve()
				.bodyToFlux(Entry.class);
		return entries.transform(this.decorator.decorate("blog-ui.streamAll"));
	}

	public Mono<BlogEntries> findByQuery(String query, Pageable pageable) {
		Mono<BlogEntries> entries = this.webClient.get() //
				.uri("entries?q={q}&page={page}&size={size}&excludeContent=true", query,
						pageable.getPageNumber(), pageable.getPageSize()) //
				.retrieve() //
				.bodyToMono(BlogEntries.class);
		return entries.transform(this.decorator.decorate("blog-ui.findByQuery"));
	}

	public Mono<BlogEntries> findByCategories(List<Category> categories,
			Pageable pageable) {
		Mono<BlogEntries> entries = this.webClient.get() //
				.uri("categories/{categories}/entries?page={page}&size={size}&excludeContent=true",
						categories.stream().map(Category::getName).collect(joining(",")),
						pageable.getPageNumber(), pageable.getPageSize()) //
				.retrieve() //
				.bodyToMono(BlogEntries.class);
		return entries.transform(this.decorator.decorate("blog-ui.findByCategories"));
	}

	public Mono<BlogEntries> findByTag(Tag tag, Pageable pageable) {
		Mono<BlogEntries> entries = this.webClient.get() //
				.uri("tags/{tag}/entries?page={page}&size={size}&excludeContent=true",
						tag.getName(), pageable.getPageNumber(), pageable.getPageSize()) //
				.retrieve() //
				.bodyToMono(BlogEntries.class);
		return entries.transform(this.decorator.decorate("blog-ui.findByTag"));
	}

	public Mono<List<Tag>> findTags() {
		Mono<List<Tag>> tags = this.webClient.get() //
				.uri("tags") //
				.retrieve() //
				.bodyToMono(new ParameterizedTypeReference<List<Tag>>() {

				});
		return tags.transform(this.decorator.decorate("blog-ui.findTags"));
	}

	public Mono<List<List<Category>>> findCategories() {
		Mono<List<List<Category>>> categories = this.webClient.get() //
				.uri("categories") //
				.retrieve() //
				.bodyToMono(new ParameterizedTypeReference<List<List<Category>>>() {

				});
		return categories.transform(this.decorator.decorate("blog-ui.findCategories"));
	}

	public void clearCache() {
		this.entryCache.invalidateAll();
	}
}
