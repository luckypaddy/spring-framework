/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.reactive.result.method.annotation;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import rx.Completable;
import rx.Single;

import org.springframework.core.MethodParameter;
import org.springframework.core.codec.ByteBufferEncoder;
import org.springframework.core.codec.CharSequenceEncoder;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.buffer.support.DataBufferTestUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.EncoderHttpMessageWriter;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.codec.ResourceHttpMessageWriter;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.http.codec.xml.Jaxb2XmlEncoder;
import org.springframework.mock.http.server.reactive.test.MockServerWebExchange;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.reactive.accept.RequestedContentTypeResolver;
import org.springframework.web.reactive.accept.RequestedContentTypeResolverBuilder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;
import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.mock.http.server.reactive.test.MockServerHttpRequest.get;
import static org.springframework.web.method.ResolvableMethod.on;
import static org.springframework.web.reactive.HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE;

/**
 * Unit tests for {@link ResponseBodyResultHandler}.When adding a test also
 * consider whether the logic under test is in a parent class, then see:
 * <ul>
 * 	<li>{@code MessageWriterResultHandlerTests},
 *  <li>{@code ContentNegotiatingResultHandlerSupportTests}
 * </ul>
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 */
public class ResponseBodyResultHandlerTests {

	private ResponseBodyResultHandler resultHandler;


	@Before
	public void setup() throws Exception {
		List<HttpMessageWriter<?>> writerList = new ArrayList<>(5);
		writerList.add(new EncoderHttpMessageWriter<>(new ByteBufferEncoder()));
		writerList.add(new EncoderHttpMessageWriter<>(CharSequenceEncoder.allMimeTypes()));
		writerList.add(new ResourceHttpMessageWriter());
		writerList.add(new EncoderHttpMessageWriter<>(new Jaxb2XmlEncoder()));
		writerList.add(new EncoderHttpMessageWriter<>(new Jackson2JsonEncoder()));
		RequestedContentTypeResolver resolver = new RequestedContentTypeResolverBuilder().build();
		this.resultHandler = new ResponseBodyResultHandler(writerList, resolver);
	}


	@Test
	public void supports() throws NoSuchMethodException {
		Object controller = new TestController();
		Method method;

		method = on(TestController.class).annotPresent(ResponseBody.class).resolveMethod();
		testSupports(controller, method);

		method = on(TestController.class).annotNotPresent(ResponseBody.class).resolveMethod("doWork");
		HandlerResult handlerResult = getHandlerResult(controller, method);
		assertFalse(this.resultHandler.supports(handlerResult));
	}

	@Test
	public void supportsRestController() throws NoSuchMethodException {
		Object controller = new TestRestController();
		Method method;

		method = on(TestRestController.class).returning(String.class).resolveMethod();
		testSupports(controller, method);

		method = on(TestRestController.class).returning(Mono.class, String.class).resolveMethod();
		testSupports(controller, method);

		method = on(TestRestController.class).returning(Single.class, String.class).resolveMethod();
		testSupports(controller, method);

		method = on(TestRestController.class).returning(Completable.class).resolveMethod();
		testSupports(controller, method);
	}

	@Test // SPR-15910
	public void handleWithPublisherWildcardBodyTypeAndResourceBody() throws Exception {

		MockServerWebExchange exchange = get("/path").toExchange();
		exchange.getAttributes().put(PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE, Collections.singleton(APPLICATION_OCTET_STREAM));

		MethodParameter returnType = on(TestRestController.class).resolveReturnType(Publisher.class);
		HandlerResult result = new HandlerResult(new TestRestController(), Mono.just(new ByteArrayResource("body".getBytes())), returnType);

		this.resultHandler.handleResult(exchange, result).block(Duration.ofSeconds(5));

		assertResponseBody(exchange, "body");
	}

	@Test // SPR-15910
	public void handleWithPublisherObjectBodyTypeAndResourceBody() throws Exception {

		MockServerWebExchange exchange = get("/path").toExchange();
		exchange.getAttributes().put(PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE, Collections.singleton(APPLICATION_OCTET_STREAM));

		MethodParameter returnType = on(TestRestController.class).resolveReturnType(Publisher.class, Object.class);
		HandlerResult result = new HandlerResult(new TestRestController(), Mono.just(new ByteArrayResource("body".getBytes())), returnType);

		this.resultHandler.handleResult(exchange, result).block(Duration.ofSeconds(5));

		assertResponseBody(exchange, "body");
	}

	@Test // SPR-15910
	public void handleWithObjectBodyTypeAndResourceBody() throws Exception {

		MockServerWebExchange exchange = get("/path").toExchange();
		exchange.getAttributes().put(PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE, Collections.singleton(APPLICATION_OCTET_STREAM));

		MethodParameter returnType = on(TestRestController.class).resolveReturnType(Object.class);
		HandlerResult result = new HandlerResult(new TestRestController(), new ByteArrayResource("body".getBytes()), returnType);

		this.resultHandler.handleResult(exchange, result).block(Duration.ofSeconds(5));

		assertResponseBody(exchange, "body");
	}

	private void testSupports(Object controller, Method method) {
		HandlerResult handlerResult = getHandlerResult(controller, method);
		assertTrue(this.resultHandler.supports(handlerResult));
	}

	private HandlerResult getHandlerResult(Object controller, Method method) {
		HandlerMethod handlerMethod = new HandlerMethod(controller, method);
		return new HandlerResult(handlerMethod, null, handlerMethod.getReturnType());
	}

	private void assertResponseBody(MockServerWebExchange exchange, String responseBody) {
		StepVerifier.create(exchange.getResponse().getBody())
				.consumeNextWith(buf -> assertEquals(responseBody,
						DataBufferTestUtils.dumpString(buf, StandardCharsets.UTF_8)))
				.expectComplete()
				.verify();
	}

	@Test
	public void defaultOrder() throws Exception {
		assertEquals(100, this.resultHandler.getOrder());
	}



	@RestController
	@SuppressWarnings("unused")
	private static class TestRestController {

		public Mono<Void> handleToMonoVoid() { return null;}

		public String handleToString() {
			return null;
		}

		public Mono<String> handleToMonoString() {
			return null;
		}

		public Single<String> handleToSingleString() {
			return null;
		}

		public Completable handleToCompletable() {
			return null;
		}

		public Publisher<?> handleToPublisherWildcard() { return null; }

		public Publisher<Object> handleToPublisherObject() { return null; }

		public Object handleToObject() { return null; }
	}


	@Controller
	@SuppressWarnings("unused")
	private static class TestController {

		@ResponseBody
		public String handleToString() {
			return null;
		}

		public String doWork() {
			return null;
		}
	}

}
