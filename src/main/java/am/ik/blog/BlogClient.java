package am.ik.blog;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_STREAM_JSON_VALUE;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.springframework.cloud.netflix.hystrix.HystrixCommands;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixObservableCommand.Setter;
import com.netflix.hystrix.exception.HystrixBadRequestException;

import am.ik.blog.entry.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class BlogClient {
	private final WebClient webClient;

	public BlogClient(WebClient.Builder builder, BlogProperties props) {
		this.webClient = builder.baseUrl(props.getApi().getUrl()).build();
	}

	public Mono<Entry> findById(Long entryId) {
		Mono<Entry> entry = this.webClient.get() //
				.uri("api/entries/{entryId}?excludeContent=false", entryId) //
				.retrieve() //
				.onStatus(HttpStatus::is4xxClientError, ignoreHystrixOnClientError()) //
				.bodyToMono(Entry.class);
		return HystrixCommands.from(entry) //
				.setter(setter("findById", 5_000)) //
				.fallback(fallbackEntry()) //
				.toMono();
	}

	public Mono<BlogEntries> findAll(Pageable pageable) {
		Mono<BlogEntries> entries = this.webClient.get() //
				.uri("api/entries?page={page}&size={size}&excludeContent=true",
						pageable.getPageNumber(), pageable.getPageSize()) //
				.retrieve() //
				.onStatus(HttpStatus::is4xxClientError, ignoreHystrixOnClientError()) //
				.bodyToMono(BlogEntries.class);
		return HystrixCommands.from(entries) //
				.setter(setter("findAll", 5_000)) //
				.fallback(fallbackEntries()) //
				.toMono();
	}

	public Flux<Entry> streamAll(Pageable pageable) {
		Flux<Entry> entries = this.webClient.get()
				.uri("api/entries?page={page}&size={size}&excludeContent=true",
						pageable.getPageNumber(), pageable.getPageSize()) //
				.header(ACCEPT, APPLICATION_STREAM_JSON_VALUE).retrieve()
				.bodyToFlux(Entry.class);
		return HystrixCommands.from(entries) //
				.setter(setter("streamAll", 5_000)) //
				.fallback(fallbackEntry().flux()) //
				.toFlux();
	}

	public Mono<BlogEntries> findByQuery(String query, Pageable pageable) {
		Mono<BlogEntries> entries = this.webClient.get() //
				.uri("api/entries?q={q}&page={page}&size={size}&excludeContent=true",
						query, pageable.getPageNumber(), pageable.getPageSize()) //
				.retrieve() //
				.onStatus(HttpStatus::is4xxClientError, ignoreHystrixOnClientError()) //
				.bodyToMono(BlogEntries.class);
		return HystrixCommands.from(entries) //
				.setter(setter("findAll", 3_000)) //
				.fallback(fallbackEntries()) //
				.toMono();
	}

	public Mono<BlogEntries> findByCategories(List<Category> categories,
			Pageable pageable) {
		Mono<BlogEntries> entries = this.webClient.get() //
				.uri("api/categories/{categories}/entries?page={page}&size={size}&excludeContent=true",
						categories.stream().map(Category::toString).collect(joining(",")),
						pageable.getPageNumber(), pageable.getPageSize()) //
				.retrieve() //
				.onStatus(HttpStatus::is4xxClientError, ignoreHystrixOnClientError()) //
				.bodyToMono(BlogEntries.class);
		return HystrixCommands.from(entries) //
				.setter(setter("findByCategories", 3_000)) //
				.fallback(fallbackEntries()) //
				.toMono();
	}

	public Mono<BlogEntries> findByTag(Tag tag, Pageable pageable) {
		Mono<BlogEntries> entries = this.webClient.get() //
				.uri("api/tags/{tag}/entries?page={page}&size={size}&excludeContent=true",
						tag, pageable.getPageNumber(), pageable.getPageSize()) //
				.retrieve() //
				.onStatus(HttpStatus::is4xxClientError, ignoreHystrixOnClientError()) //
				.bodyToMono(BlogEntries.class);
		return HystrixCommands.from(entries) //
				.setter(setter("findByTag", 3_000)) //
				.fallback(fallbackEntries()) //
				.toMono();
	}

	public Mono<List<Tag>> findTags() {
		Mono<List<Tag>> tags = this.webClient.get() //
				.uri("api/tags") //
				.retrieve() //
				.onStatus(HttpStatus::is4xxClientError, ignoreHystrixOnClientError()) //
				.bodyToMono(new ParameterizedTypeReference<List<String>>() {
				}) //
				.map(s -> s.stream() //
						.map(Tag::new) //
						.collect(toList()));
		return HystrixCommands.from(tags) //
				.setter(setter("findTags", 3_000)) //
				.fallback(fallbackTags()) //
				.toMono();
	}

	public Mono<List<Categories>> findCategories() {
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
		return HystrixCommands.from(categories) //
				.setter(setter("findCategories", 3_000)) //
				.fallback(fallbackCategories()) //
				.toMono();
	}

	private Setter setter(String commandName, int timeout) {
		HystrixCommandGroupKey groupKey = HystrixCommandGroupKey.Factory
				.asKey(BlogClient.class.getSimpleName());
		HystrixCommandKey commandKey = HystrixCommandKey.Factory.asKey(commandName);
		HystrixCommandProperties.Setter setter = HystrixCommandProperties.defaultSetter()
				.withExecutionTimeoutInMilliseconds(timeout);
		return Setter.withGroupKey(groupKey) //
				.andCommandKey(commandKey) //
				.andCommandPropertiesDefaults(setter);
	}

	private Function<ClientResponse, Mono<? extends Throwable>> ignoreHystrixOnClientError() {
		return r -> ignoredException(new ResponseStatusException(r.statusCode()));
	}

	private Mono<? extends Throwable> ignoredException(Throwable e) {
		return Mono.error(new HystrixBadRequestException(e.getMessage(), e));
	}

	private Mono<Entry> fallbackEntry() {
		return Mono.fromCallable(() -> {
			EventTime now = EventTime.now();
			Author author = new Author(new Name("system"), now);
			return Entry.builder() //
					.entryId(new EntryId(0L)) //
					.frontMatter(new FrontMatter(new Title("Service is unavailable now"),
							new Categories(Arrays.asList()), new Tags(Arrays.asList()),
							now, now, PremiumPoint.UNSET)) //
					.content(new Content("I'm so sorry for the inconvenience.")) //
					.created(author) //
					.updated(author) //
					.build();
		}).cache();
	}

	private Mono<BlogEntries> fallbackEntries() {
		return this.fallbackEntry() //
				.map(e -> new BlogEntries(singletonList(e)));
	}

	private Mono<List<Categories>> fallbackCategories() {
		return Mono.fromCallable(() -> singletonList(new Categories(
				singletonList(new Category("Service is unavailable now")))));
	}

	private Mono<List<Tag>> fallbackTags() {
		return Mono
				.fromCallable(() -> singletonList(new Tag("Service is unavailable now")));
	}
}
