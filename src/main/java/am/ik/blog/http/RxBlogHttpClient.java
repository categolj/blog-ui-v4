package am.ik.blog.http;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import am.ik.blog.BlogEntries;
import am.ik.blog.BlogProperties;
import am.ik.blog.entry.Categories;
import am.ik.blog.entry.Category;
import am.ik.blog.entry.Entry;
import am.ik.blog.entry.EntryId;
import am.ik.blog.entry.Tag;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;
import com.netflix.hystrix.exception.HystrixBadRequestException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.cache.CacheMono;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Signal;
import rx.Observable;
import rx.RxReactiveStreams;
import rx.Single;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.HttpHeaders.IF_MODIFIED_SINCE;
import static org.springframework.http.HttpStatus.NOT_MODIFIED;

@Component
public class RxBlogHttpClient {
	private final WebClient webClient;
	private final Cache<EntryId, Entry> entryCache;
	private static final Logger log = LoggerFactory.getLogger(RxBlogHttpClient.class);

	public RxBlogHttpClient(WebClient.Builder builder, MeterRegistry meterRegistry,
			BlogProperties props) {
		this.webClient = builder.baseUrl(props.getApi().getUrl()).build();
		this.entryCache = CaffeineCacheMetrics.monitor(meterRegistry, Caffeine
				.newBuilder() //
				.maximumSize(100) //
				.expireAfterWrite(3, TimeUnit.DAYS) //
				.removalListener(
						(key, value, cause) -> log.info("Remove cache(entryId={})", key)) //
				.build(), "entryCache");
	}

	@HystrixCommand(commandProperties = {
			@HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "3000"), //
			@HystrixProperty(name = "execution.isolation.semaphore.maxConcurrentRequests", value = "40"), //
	})
	public Single<Entry> findById(Long entryId) {
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
						.onStatus(HttpStatus::is4xxClientError,
								ignoreHystrixOnClientError()) //
						.bodyToMono(Entry.class)) //
				.andWriteWith((key, signal) -> Mono.fromRunnable(() -> this.entryCache
						.put(key, Objects.requireNonNull(signal.get()))));
		return RxReactiveStreams.toSingle(entry);
	}

	@HystrixCommand(commandProperties = {
			@HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "3000"), //
	})
	public Single<BlogEntries> findAll(Pageable pageable) {
		Mono<BlogEntries> entries = this.webClient.get() //
				.uri("api/entries?page={page}&size={size}&excludeContent=true",
						pageable.getPageNumber(), pageable.getPageSize()) //
				.retrieve() //
				.onStatus(HttpStatus::is4xxClientError, ignoreHystrixOnClientError()) //
				.bodyToMono(BlogEntries.class);
		return RxReactiveStreams.toSingle(entries);
	}

	@HystrixCommand(commandProperties = {
			@HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "5000"), //
			@HystrixProperty(name = "execution.isolation.semaphore.maxConcurrentRequests", value = "30"), //
	})
	public Observable<Entry> streamAll(Pageable pageable) {
		Flux<Entry> entries = this.webClient.get()
				.uri("api/entries?page={page}&size={size}&excludeContent=true",
						pageable.getPageNumber(), pageable.getPageSize()) //
				.header(ACCEPT, "application/stream+x-jackson-smile").retrieve()
				.bodyToFlux(Entry.class);
		return RxReactiveStreams.toObservable(entries);
	}

	@HystrixCommand(commandProperties = {
			@HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "3000"), //
	})
	public Single<BlogEntries> findByQuery(String query, Pageable pageable) {
		Mono<BlogEntries> entries = this.webClient.get() //
				.uri("api/entries?q={q}&page={page}&size={size}&excludeContent=true",
						query, pageable.getPageNumber(), pageable.getPageSize()) //
				.retrieve() //
				.onStatus(HttpStatus::is4xxClientError, ignoreHystrixOnClientError()) //
				.bodyToMono(BlogEntries.class);
		return RxReactiveStreams.toSingle(entries);
	}

	@HystrixCommand(commandProperties = {
			@HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "3000"), //
	})
	public Single<BlogEntries> findByCategories(List<Category> categories,
			Pageable pageable) {
		Mono<BlogEntries> entries = this.webClient.get() //
				.uri("api/categories/{categories}/entries?page={page}&size={size}&excludeContent=true",
						categories.stream().map(Category::toString).collect(joining(",")),
						pageable.getPageNumber(), pageable.getPageSize()) //
				.retrieve() //
				.onStatus(HttpStatus::is4xxClientError, ignoreHystrixOnClientError()) //
				.bodyToMono(BlogEntries.class);
		return RxReactiveStreams.toSingle(entries);
	}

	@HystrixCommand(commandProperties = {
			@HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "3000"), //
	})
	public Single<BlogEntries> findByTag(Tag tag, Pageable pageable) {
		Mono<BlogEntries> entries = this.webClient.get() //
				.uri("api/tags/{tag}/entries?page={page}&size={size}&excludeContent=true",
						tag, pageable.getPageNumber(), pageable.getPageSize()) //
				.retrieve() //
				.onStatus(HttpStatus::is4xxClientError, ignoreHystrixOnClientError()) //
				.bodyToMono(BlogEntries.class);
		return RxReactiveStreams.toSingle(entries);
	}

	@HystrixCommand(commandProperties = {
			@HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "2000"), //
	})
	public Single<List<Tag>> findTags() {
		Mono<List<Tag>> tags = this.webClient.get() //
				.uri("api/tags") //
				.retrieve() //
				.onStatus(HttpStatus::is4xxClientError, ignoreHystrixOnClientError()) //
				.bodyToMono(new ParameterizedTypeReference<List<String>>() {
				}) //
				.map(s -> s.stream() //
						.map(Tag::new) //
						.collect(toList()));
		return RxReactiveStreams.toSingle(tags);
	}

	@HystrixCommand(commandProperties = {
			@HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "2000"), //
	})
	public Single<List<Categories>> findCategories() {
		Mono<List<Categories>> categories = this.webClient.get() //
				.uri("api/categories") //
				.retrieve() //
				.onStatus(HttpStatus::is4xxClientError, ignoreHystrixOnClientError()) //
				.bodyToMono(new ParameterizedTypeReference<List<List<String>>>() {
				}) //
				.map(s -> s.stream() //
						.map(c -> new Categories(c.stream() //
								.map(Category::new) //
								.collect(toList()))) //
						.collect(toList()));
		return RxReactiveStreams.toSingle(categories);
	}

	public void clearCache() {
		this.entryCache.invalidateAll();
	}

	private Function<ClientResponse, Mono<? extends Throwable>> ignoreHystrixOnClientError() {
		return r -> ignoredException(new ResponseStatusException(r.statusCode()));
	}

	private Mono<? extends Throwable> ignoredException(Throwable e) {
		return Mono.error(new HystrixBadRequestException(e.getMessage(), e));
	}
}
