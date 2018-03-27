package org.springframework.http.client.reactive;

import java.net.URI;
import java.util.function.Function;

import jdk.incubator.http.HttpClient;
import reactor.core.publisher.Mono;

import org.springframework.http.HttpMethod;

public class JdkClientHttpConnector implements ClientHttpConnector {

	private final HttpClient client;

	public JdkClientHttpConnector() {
		this.client = HttpClient.newHttpClient();
	}

	@Override
	public Mono<ClientHttpResponse> connect(HttpMethod method, URI uri, Function<? super ClientHttpRequest, Mono<Void>> requestCallback) {
		JdkClientHttpRequest request = new JdkClientHttpRequest(method, uri, this.client);
		return requestCallback.apply(request).thenReturn(new JdkClientHttpResponse(request.getHttpResponse(), request.getContent()));
	}
}
