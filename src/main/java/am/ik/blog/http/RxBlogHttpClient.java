package am.ik.blog.http;

import java.util.List;
import java.util.function.Function;

import am.ik.blog.BlogEntries;
import am.ik.blog.BlogProperties;
import am.ik.blog.entry.Categories;
import am.ik.blog.entry.Category;
import am.ik.blog.entry.Entry;
import am.ik.blog.entry.Tag;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;
import com.netflix.hystrix.exception.HystrixBadRequestException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
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

@Component
public class RxBlogHttpClient {
	private final WebClient webClient;

	public RxBlogHttpClient(WebClient.Builder builder, BlogProperties props) {
		this.webClient = builder.baseUrl(props.getApi().getUrl()).build();
	}

	@HystrixCommand(commandProperties = {
			@HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "3000"), //
			@HystrixProperty(name = "execution.isolation.semaphore.maxConcurrentRequests", value = "20"), //
	})
	public Single<Entry> findById(Long entryId) {
		Mono<Entry> entry = this.webClient.get() //
				.uri("api/entries/{entryId}?excludeContent=false", entryId) //
				.retrieve() //
				.onStatus(HttpStatus::is4xxClientError, ignoreHystrixOnClientError()) //
				.bodyToMono(Entry.class);
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

	private Function<ClientResponse, Mono<? extends Throwable>> ignoreHystrixOnClientError() {
		return r -> ignoredException(new ResponseStatusException(r.statusCode()));
	}

	private Mono<? extends Throwable> ignoredException(Throwable e) {
		return Mono.error(new HystrixBadRequestException(e.getMessage(), e));
	}
}
