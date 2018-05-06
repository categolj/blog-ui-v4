package am.ik.blog.http;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import am.ik.blog.BlogClient;
import am.ik.blog.BlogEntries;
import am.ik.blog.BlogProperties;
import am.ik.blog.entry.*;
import com.netflix.hystrix.exception.HystrixBadRequestException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.netflix.hystrix.HystrixCommands;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.springframework.http.HttpHeaders.ACCEPT;

@Component
@ConditionalOnProperty(name = "blog.client.type", havingValue = "http", matchIfMissing = true)
public class BlogHttpClient implements BlogClient {
	private final WebClient webClient;

	public BlogHttpClient(WebClient.Builder builder, BlogProperties props) {
		this.webClient = builder.baseUrl(props.getApi().getUrl()).build();
	}

	@Override
	public Mono<Entry> findById(Long entryId) {
		Mono<Entry> entry = this.webClient.get() //
				.uri("api/entries/{entryId}?excludeContent=false", entryId) //
				.retrieve() //
				.onStatus(HttpStatus::is4xxClientError, ignoreHystrixOnClientError()) //
				.bodyToMono(Entry.class);
		return HystrixCommands.from(entry) //
				.groupName(BlogHttpClient.class.getSimpleName()) //
				.commandName("findById") //
				.commandProperties(p -> p.withExecutionTimeoutInMilliseconds(5_000)) //
				.fallback(fallbackEntry()) //
				.toMono() //
				.transform(this::unwrapIgnoredException);
	}

	@Override
	public Mono<BlogEntries> findAll(Pageable pageable) {
		Mono<BlogEntries> entries = this.webClient.get() //
				.uri("api/entries?page={page}&size={size}&excludeContent=true",
						pageable.getPageNumber(), pageable.getPageSize()) //
				.retrieve() //
				.onStatus(HttpStatus::is4xxClientError, ignoreHystrixOnClientError()) //
				.bodyToMono(BlogEntries.class);
		return HystrixCommands.from(entries) //
				.groupName(BlogHttpClient.class.getSimpleName()) //
				.commandName("findAll") //
				.commandProperties(p -> p.withExecutionTimeoutInMilliseconds(5_000)) //
				.fallback(fallbackEntries()) //
				.toMono() //
				.transform(this::unwrapIgnoredException);
	}

	@Override
	public Flux<Entry> streamAll(Pageable pageable) {
		Flux<Entry> entries = this.webClient.get()
				.uri("api/entries?page={page}&size={size}&excludeContent=true",
						pageable.getPageNumber(), pageable.getPageSize()) //
				.header(ACCEPT, "application/stream+x-jackson-smile").retrieve()
				.bodyToFlux(Entry.class);
		return HystrixCommands.from(entries) //
				.groupName(BlogHttpClient.class.getSimpleName()) //
				.commandName("streamAll") //
				.commandProperties(p -> p.withExecutionTimeoutInMilliseconds(5_000)) //
				.fallback(fallbackEntry().flux()) //
				.toFlux() //
				.transform(this::unwrapIgnoredException);
	}

	@Override
	public Mono<BlogEntries> findByQuery(String query, Pageable pageable) {
		Mono<BlogEntries> entries = this.webClient.get() //
				.uri("api/entries?q={q}&page={page}&size={size}&excludeContent=true",
						query, pageable.getPageNumber(), pageable.getPageSize()) //
				.retrieve() //
				.onStatus(HttpStatus::is4xxClientError, ignoreHystrixOnClientError()) //
				.bodyToMono(BlogEntries.class);
		return HystrixCommands.from(entries) //
				.groupName(BlogHttpClient.class.getSimpleName()) //
				.commandName("findAll") //
				.commandProperties(p -> p.withExecutionTimeoutInMilliseconds(3_000)) //
				.fallback(fallbackEntries()) //
				.toMono() //
				.transform(this::unwrapIgnoredException);
	}

	@Override
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
				.groupName(BlogHttpClient.class.getSimpleName()) //
				.commandName("findByCategories") //
				.commandProperties(p -> p.withExecutionTimeoutInMilliseconds(3_000)) //
				.fallback(fallbackEntries()) //
				.toMono() //
				.transform(this::unwrapIgnoredException);
	}

	@Override
	public Mono<BlogEntries> findByTag(Tag tag, Pageable pageable) {
		Mono<BlogEntries> entries = this.webClient.get() //
				.uri("api/tags/{tag}/entries?page={page}&size={size}&excludeContent=true",
						tag, pageable.getPageNumber(), pageable.getPageSize()) //
				.retrieve() //
				.onStatus(HttpStatus::is4xxClientError, ignoreHystrixOnClientError()) //
				.bodyToMono(BlogEntries.class);
		return HystrixCommands.from(entries) //
				.groupName(BlogHttpClient.class.getSimpleName()) //
				.commandName("findByTag") //
				.commandProperties(p -> p.withExecutionTimeoutInMilliseconds(3_000)) //
				.fallback(fallbackEntries()) //
				.toMono() //
				.transform(this::unwrapIgnoredException);
	}

	@Override
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
				.groupName(BlogHttpClient.class.getSimpleName()) //
				.commandName("findTags") //
				.commandProperties(p -> p.withExecutionTimeoutInMilliseconds(3_000)) //
				.fallback(fallbackTags()) //
				.toMono() //
				.transform(this::unwrapIgnoredException);
	}

	@Override
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
				.groupName(BlogHttpClient.class.getSimpleName()) //
				.commandName("findCategories") //
				.commandProperties(p -> p.withExecutionTimeoutInMilliseconds(3_000)) //
				.fallback(fallbackCategories()) //
				.toMono() //
				.transform(this::unwrapIgnoredException);
	}

	private Function<ClientResponse, Mono<? extends Throwable>> ignoreHystrixOnClientError() {
		return r -> ignoredException(new ResponseStatusException(r.statusCode()));
	}

	private Mono<? extends Throwable> ignoredException(Throwable e) {
		return Mono.error(new HystrixBadRequestException(e.getMessage(), e));
	}

	private <T> Mono<T> unwrapIgnoredException(Mono<T> m) {
		return m.onErrorMap(HystrixBadRequestException.class, Throwable::getCause);
	}

	private <T> Flux<T> unwrapIgnoredException(Flux<T> f) {
		return f.onErrorMap(HystrixBadRequestException.class, Throwable::getCause);
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
