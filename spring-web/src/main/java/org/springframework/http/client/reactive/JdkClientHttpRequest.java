package org.springframework.http.client.reactive;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.Flow;

import jdk.incubator.http.HttpClient;
import jdk.incubator.http.HttpRequest;
import jdk.incubator.http.HttpResponse;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.DirectProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxProcessor;
import reactor.core.publisher.Mono;

import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpMethod;

public class JdkClientHttpRequest extends AbstractClientHttpRequest {

	private final HttpMethod httpMethod;

	private final URI uri;

	private final HttpRequest.Builder builder;

	private final ReactiveAdapter flowAdapter;

	private final HttpClient httpClient;

	private HttpResponse<Void> httpResponse;

	private Flux<DataBuffer> content;


	public JdkClientHttpRequest(HttpMethod httpMethod, URI uri, HttpClient httpClient) {
		this.uri = uri;
		this.httpMethod = httpMethod;
		// TODO For now we use HTTP/1.1
		this.builder = HttpRequest.newBuilder(uri).version(HttpClient.Version.HTTP_1_1);
		this.flowAdapter = ReactiveAdapterRegistry.getSharedInstance().getAdapter(Flow.Publisher.class);
		this.httpClient = httpClient;
	}

	@Override
	protected void applyHeaders() {
		getHeaders().entrySet().forEach(e -> e.getValue().forEach(v -> builder.header(e.getKey(), v)));
	}

	@Override
	protected void applyCookies() {
		throw new UnsupportedOperationException();
	}

	@Override
	public HttpMethod getMethod() {
		return this.httpMethod;
	}

	@Override
	public URI getURI() {
		return this.uri;
	}

	@Override
	public DataBufferFactory bufferFactory() {
		return new DefaultDataBufferFactory();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
		return doCommit(() -> {
			Flow.Publisher<ByteBuffer> publisher = (Flow.Publisher<ByteBuffer>) flowAdapter.fromPublisher(Flux.from(body).map(DataBuffer::asByteBuffer));
			HttpRequest request = this.builder.method(this.httpMethod.name(), HttpRequest.BodyPublisher.fromPublisher(publisher)).build();
			FluxProcessor<List<ByteBuffer>, List<ByteBuffer>> processor = DirectProcessor.create();
			HttpResponse.BodyHandler<Void> bodyHandler = HttpResponse.BodyHandler.fromSubscriber(new SubscriberToRS<>(processor));
			this.content = processor.flatMap(Flux::fromIterable).map(buffer -> this.bufferFactory().wrap(buffer));;
			return Mono.fromFuture(httpClient.sendAsync(request, bodyHandler)).doOnSuccess(response -> this.httpResponse = response).then();
		});
	}

	@Override
	public Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Mono<Void> setComplete() {
		return doCommit(() -> {
			HttpRequest request = this.builder.method(this.httpMethod.name(), HttpRequest.BodyPublisher.noBody()).build();
			FluxProcessor<List<ByteBuffer>, List<ByteBuffer>> processor = DirectProcessor.create();
			HttpResponse.BodyHandler<Void> bodyHandler = HttpResponse.BodyHandler.fromSubscriber(new SubscriberToRS<>(processor));
			this.content = processor.flatMap(Flux::fromIterable).map(buffer -> this.bufferFactory().wrap(buffer));
			return Mono.fromFuture(httpClient.sendAsync(request, bodyHandler)).doOnSuccess(response -> this.httpResponse = response).then();
		});
	}

	public HttpResponse<Void> getHttpResponse() {
		return httpResponse;
	}

	public Flux<DataBuffer> getContent() {
		return content;
	}

	private static class SubscriberToRS<T> implements Flow.Subscriber<T>, Subscription {

		private final Subscriber<? super T> s;

		Flow.Subscription subscription;

		public SubscriberToRS(Subscriber<? super T> s) {
			this.s = s;
		}

		@Override
		public void onSubscribe(final Flow.Subscription subscription) {
			this.subscription = subscription;
			s.onSubscribe(this);
		}

		@Override
		public void onNext(T o) {
			s.onNext(o);
		}

		@Override
		public void onError(Throwable throwable) {
			s.onError(throwable);
		}

		@Override
		public void onComplete() {
			s.onComplete();
		}

		@Override
		public void request(long n) {
			subscription.request(n);
		}

		@Override
		public void cancel() {
			subscription.cancel();
		}
	}

}
