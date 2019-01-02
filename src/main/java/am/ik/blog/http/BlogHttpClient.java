package am.ik.blog.http;

import java.util.List;
import java.util.concurrent.TimeUnit;

import am.ik.blog.BlogClient;
import am.ik.blog.BlogEntries;
import am.ik.blog.BlogProperties;
import am.ik.blog.entry.Categories;
import am.ik.blog.entry.Category;
import am.ik.blog.entry.Entry;
import am.ik.blog.entry.EntryId;
import am.ik.blog.entry.Tag;
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
import static java.util.stream.Collectors.toList;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.HttpHeaders.IF_MODIFIED_SINCE;
import static org.springframework.http.HttpStatus.NOT_MODIFIED;

@Component
public class BlogHttpClient implements BlogClient {
	private final WebClient webClient;
	private final Cache<EntryId, Entry> entryCache;
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
		this.retryer = new Retryer(tracer);
		this.webClient = builder.baseUrl(props.getApi().getUrl()) //
				.build();
	}

	public Mono<Entry> findById(Long entryId) {
		Mono<Entry> entry = CacheMono
				.lookup(key -> Mono.justOrEmpty(this.entryCache.get(key, x -> null)) //
						.flatMap(e -> this.webClient.head() //
								.uri("api/entries/{entryId}", entryId) //
								.header(IF_MODIFIED_SINCE,
										e.getUpdated().getDate().rfc1123()) //
								.exchange() //
								.filter(r -> r.statusCode() == NOT_MODIFIED) //
								.map(x -> e))
						.map(Signal::next), new EntryId(entryId)) //
				.onCacheMissResume(() -> this.webClient.get() //
						.uri("api/entries/{entryId}?excludeContent=false", entryId) //
						.retrieve() //
						.bodyToMono(Entry.class)) //
				.andWriteWith((key, signal) -> Mono.justOrEmpty(signal.get())
						.doOnNext(e -> this.entryCache.put(key, e)).then());
		return this.decorate(entry, "blog-ui.findById");
	}

	public Mono<BlogEntries> findAll(Pageable pageable) {
		Mono<BlogEntries> entries = this.webClient.get() //
				.uri("api/entries?page={page}&size={size}&excludeContent=true",
						pageable.getPageNumber(), pageable.getPageSize()) //
				.retrieve() //
				.bodyToMono(BlogEntries.class);
		return this.decorate(entries, "blog-ui.findAll");
	}

	public Flux<Entry> streamAll(Pageable pageable) {
		Flux<Entry> entries = this.webClient.get()
				.uri("api/entries?page={page}&size={size}&excludeContent=true",
						pageable.getPageNumber(), pageable.getPageSize()) //
				.header(ACCEPT, "application/stream+x-jackson-smile").retrieve()
				.bodyToFlux(Entry.class);
		return this.decorate(entries, "blog-ui.streamAll");
	}

	public Mono<BlogEntries> findByQuery(String query, Pageable pageable) {
		Mono<BlogEntries> entries = this.webClient.get() //
				.uri("api/entries?q={q}&page={page}&size={size}&excludeContent=true",
						query, pageable.getPageNumber(), pageable.getPageSize()) //
				.retrieve() //
				.bodyToMono(BlogEntries.class);
		return this.decorate(entries, "blog-ui.findByQuery");
	}

	public Mono<BlogEntries> findByCategories(List<Category> categories,
			Pageable pageable) {
		Mono<BlogEntries> entries = this.webClient.get() //
				.uri("api/categories/{categories}/entries?page={page}&size={size}&excludeContent=true",
						categories.stream().map(Category::toString).collect(joining(",")),
						pageable.getPageNumber(), pageable.getPageSize()) //
				.retrieve() //
				.bodyToMono(BlogEntries.class);
		return this.decorate(entries, "blog-ui.findByCategories");
	}

	public Mono<BlogEntries> findByTag(Tag tag, Pageable pageable) {
		Mono<BlogEntries> entries = this.webClient.get() //
				.uri("api/tags/{tag}/entries?page={page}&size={size}&excludeContent=true",
						tag, pageable.getPageNumber(), pageable.getPageSize()) //
				.retrieve() //
				.bodyToMono(BlogEntries.class);
		return this.decorate(entries, "blog-ui.findByTag");
	}

	public Mono<List<Tag>> findTags() {
		Mono<List<Tag>> tags = this.webClient.get() //
				.uri("api/tags") //
				.retrieve() //
				.bodyToMono(new ParameterizedTypeReference<List<String>>() {
				}) //
				.map(s -> s.stream() //
						.map(Tag::new) //
						.collect(toList()));
		return this.decorate(tags, "blog-ui.findTags");
	}

	public Mono<List<Categories>> findCategories() {
		Mono<List<Categories>> categories = this.webClient.get() //
				.uri("api/categories") //
				.retrieve() //
				.bodyToMono(new ParameterizedTypeReference<List<List<String>>>() {
				}) //
				.map(s -> s.stream() //
						.map(c -> new Categories(c.stream() //
								.map(Category::new) //
								.collect(toList()))) //
						.collect(toList()));
		return this.decorate(categories, "blog-ui.findCategories");
	}

	<T> Mono<T> decorate(Mono<T> mono, String name) {
		return this.circuitBreakerFactory.create(name)
				.run(mono.transform(this.retryer.retry(name)));
	}

	<T> Flux<T> decorate(Flux<T> flux, String name) {
		return this.circuitBreakerFactory.create(name)
				.run(flux.transform(this.retryer.retry(name)));
	}

	public void clearCache() {
		this.entryCache.invalidateAll();
	}
}
