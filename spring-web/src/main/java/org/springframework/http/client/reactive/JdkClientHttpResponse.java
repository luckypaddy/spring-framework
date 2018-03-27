package org.springframework.http.client.reactive;

import jdk.incubator.http.HttpResponse;
import reactor.core.publisher.Flux;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.util.MultiValueMap;

public class JdkClientHttpResponse implements ClientHttpResponse {

	private final HttpResponse<?> httpResponse;

	private final Flux<DataBuffer> content;

	public JdkClientHttpResponse(HttpResponse<?> httpResponse, Flux<DataBuffer> content) {
		this.httpResponse = httpResponse;
		this.content = content;
	}

	@Override
	public HttpStatus getStatusCode() {
		return HttpStatus.valueOf(this.httpResponse.statusCode());
	}

	@Override
	public MultiValueMap<String, ResponseCookie> getCookies() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Flux<DataBuffer> getBody() {
		return this.content;
	}

	@Override
	public HttpHeaders getHeaders() {
		HttpHeaders headers = new HttpHeaders();
		this.httpResponse.headers().map().entrySet()
				.forEach(e -> e.getValue().forEach(v -> headers.add(e.getKey(), v)));
		return headers;
	}
}
