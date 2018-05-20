package am.ik.blog.rsocket;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.annotation.PostConstruct;

import am.ik.blog.BlogClient;
import am.ik.blog.BlogEntries;
import am.ik.blog.entry.Categories;
import am.ik.blog.entry.Category;
import am.ik.blog.entry.Entry;
import am.ik.blog.entry.Tag;
import brave.Span;
import brave.Tracer;
import brave.propagation.TraceContext;
import com.fasterxml.jackson.databind.JsonNode;
import io.netty.buffer.PooledByteBufAllocator;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.RSocketFactory;
import io.rsocket.exceptions.ApplicationErrorException;
import io.rsocket.exceptions.RSocketException;
import io.rsocket.transport.ClientTransport;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.util.DefaultPayload;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@ConditionalOnProperty(name = "blog.client.type", havingValue = "rsocket")
@ConfigurationProperties(prefix = "blog.rsocket")
public class BlogRSocketClient implements BlogClient {
	private ClientTransport transport;
	private URI uri;
	private RSocketCodec codec = RSocketCodec.SMILE;
	private DataBufferFactory dataBufferFactory = new NettyDataBufferFactory(
			PooledByteBufAllocator.DEFAULT);
	private final Tracer tracer;
	private static final Logger log = LoggerFactory.getLogger(BlogRSocketClient.class);

	public BlogRSocketClient(Tracer tracer) {
		this.tracer = tracer;
	}

	@PostConstruct
	private void init() {
		this.transport = TcpClientTransport.create(this.uri.getHost(),
				this.uri.getPort());
	}

	private Mono<RSocket> rsocket() {
		return RSocketFactory.connect().transport(this.transport).start();
	}

	@Override
	public Mono<Entry> findById(Long entryId) {
		return this.rsocket()
				.flatMap(rs -> rs
						.requestResponse(DefaultPayload.create("", "/entries/" + entryId))
						.onErrorMap(RSocketException.class, this::convertException) //
						.transform(this::toEntryMono));
	}

	@Override
	public Flux<Entry> streamAll(Pageable pageable) {
		Span span = this.tracer.nextSpan().name("streamAll");
		log.info("start {}", span);
		TraceContext context = span.context();
		UriComponents uri = UriComponentsBuilder.newInstance()
				.queryParam("page", pageable.getPageNumber()) //
				.queryParam("size", pageable.getPageSize()) //
				.queryParam("X-B3-TraceId", context.traceIdString()) //
				.queryParam("X-B3-SpanId", Long.toHexString(context.spanId())) //
				.queryParam("X-B3-ParentSpanId", Long.toHexString(context.parentId())) //
				.queryParam("X-B3-Sampled", context.sampled()) //
				.build();
		return this.rsocket()
				.flatMapMany(rs -> rs
						.requestStream(DefaultPayload.create(uri.getQuery(), "/entries"))
						.onErrorMap(RSocketException.class, this::convertException) //
						.transform(this::toEntryFlux))
				.doOnTerminate(() -> {
					log.info("finish {}", span);
					span.finish();
				});
	}

	@Override
	public Flux<Entry> streamByQuery(String query, Pageable pageable) {
		UriComponents uri = UriComponentsBuilder.newInstance()
				.queryParam("page", pageable.getPageNumber()) //
				.queryParam("size", pageable.getPageSize()) //
				.queryParam("q", query) //
				.build();
		return this.rsocket()
				.flatMapMany(rs -> rs
						.requestStream(DefaultPayload.create(uri.getQuery(), "/entries"))
						.onErrorMap(RSocketException.class, this::convertException) //
						.transform(this::toEntryFlux));
	}

	@Override
	public Flux<Entry> streamByCategories(List<Category> categories, Pageable pageable) {
		UriComponents uri = UriComponentsBuilder.newInstance()
				.queryParam("page", pageable.getPageNumber()) //
				.queryParam("size", pageable.getPageSize()) //
				.build();
		return this.rsocket()
				.flatMapMany(rs -> rs
						.requestStream(DefaultPayload.create(uri.getQuery(),
								String.format("/categories/%s/entries",
										categories.stream().map(Category::getValue)
												.collect(Collectors.joining(",")))))
						.onErrorMap(RSocketException.class, this::convertException) //
						.transform(this::toEntryFlux));
	}

	@Override
	public Flux<Entry> streamByTag(Tag tag, Pageable pageable) {
		UriComponents uri = UriComponentsBuilder.newInstance()
				.queryParam("page", pageable.getPageNumber()) //
				.queryParam("size", pageable.getPageSize()) //
				.build();
		return this.rsocket()
				.flatMapMany(rs -> rs
						.requestStream(DefaultPayload.create(uri.getQuery(),
								String.format("/tags/%s/entries", tag)))
						.onErrorMap(RSocketException.class, this::convertException) //
						.transform(this::toEntryFlux));
	}

	@Override
	public Flux<Tag> streamTags() {
		return this.rsocket()
				.flatMapMany(rs -> rs.requestResponse(DefaultPayload.create("", "/tags"))
						.onErrorMap(RSocketException.class, this::convertException) //
						.transform(this::toTagsMono) //
						.flatMapMany(Flux::fromIterable));
	}

	@Override
	public Flux<Categories> streamCategories() {
		return this.rsocket()
				.flatMapMany(rs -> rs
						.requestResponse(DefaultPayload.create("", "/categories"))
						.onErrorMap(RSocketException.class, this::convertException) //
						.transform(this::toCategoriesMono) //
						.flatMapMany(Flux::fromIterable));
	}

	private Flux<Entry> toEntryFlux(Publisher<Payload> payload) {
		return this.toFlux(payload, ResolvableType.forType(Entry.class))
				.cast(Entry.class);
	}

	private Mono<Entry> toEntryMono(Publisher<Payload> payload) {
		return this.toMono(payload, ResolvableType.forType(Entry.class))
				.cast(Entry.class);
	}

	private Mono<List<Tag>> toTagsMono(Publisher<Payload> payload) {
		return this.toFlux(payload, ResolvableType.forType(Tag.class)) //
				.cast(Tag.class) //
				.collectList();
	}

	private Mono<List<Categories>> toCategoriesMono(Publisher<Payload> payload) {
		return this.toFlux(payload, ResolvableType.forType(JsonNode.class))
				.cast(JsonNode.class) //
				.map(n -> new Categories( // TODO deserializer
						StreamSupport.stream(n.get("categories").spliterator(), false)
								.map(JsonNode::asText).map(Category::new)
								.collect(Collectors.toList())))
				.collectList();
	}

	private Flux<Object> toFlux(Publisher<Payload> payload, ResolvableType type) {
		return this.codec.decoder()
				.decode(Flux.from(payload).map(Payload::getData)
						.map(x -> dataBufferFactory.wrap(x)), type,
						this.codec.streamMediaType(), Collections.emptyMap());
	}

	private Mono<Object> toMono(Publisher<Payload> payload, ResolvableType type) {
		return this.codec.decoder()
				.decode(Flux.from(payload).map(Payload::getData)
						.map(x -> dataBufferFactory.wrap(x)), type,
						this.codec.singleMediaType(), Collections.emptyMap()) //
				.next();
	}

	private ResponseStatusException convertException(RSocketException e) {
		String message = e.getMessage();
		if (e instanceof ApplicationErrorException) {
			Matcher matcher = Pattern
					.compile("Response status (\\d+) with reason \"(.+)\"")
					.matcher(message);
			if (matcher.matches()) {
				int statusCode = Integer.parseInt(matcher.group(1));
				return new ResponseStatusException(HttpStatus.valueOf(statusCode),
						matcher.group(2));
			}
		}
		return new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, message, e);
	}

	// Old API

	@Override
	public Mono<BlogEntries> findAll(Pageable pageable) {
		return this.streamAll(pageable).collectList().map(BlogEntries::new);
	}

	@Override
	public Mono<BlogEntries> findByQuery(String query, Pageable pageable) {
		return this.streamByQuery(query, pageable).collectList().map(BlogEntries::new);
	}

	@Override
	public Mono<BlogEntries> findByCategories(List<Category> categories,
			Pageable pageable) {
		return this.streamByCategories(categories, pageable).collectList()
				.map(BlogEntries::new);
	}

	@Override
	public Mono<BlogEntries> findByTag(Tag tag, Pageable pageable) {
		return this.streamByTag(tag, pageable).collectList().map(BlogEntries::new);
	}

	@Override
	public Mono<List<Tag>> findTags() {
		return this.streamTags().collectList();
	}

	@Override
	public Mono<List<Categories>> findCategories() {
		return this.streamCategories().collectList();
	}

	public URI getUri() {
		return uri;
	}

	public void setUri(URI uri) {
		this.uri = uri;
	}

	public RSocketCodec getCodec() {
		return codec;
	}

	public void setCodec(RSocketCodec codec) {
		this.codec = codec;
	}
}
