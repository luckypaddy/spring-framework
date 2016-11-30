/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.reactive.result.view.freemarker;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Locale;

import freemarker.template.Configuration;
import org.junit.Before;
import org.junit.Test;
import reactor.test.StepVerifier;

import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerHttpResponse;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.ModelMap;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.session.DefaultWebSessionManager;
import org.springframework.web.server.session.WebSessionManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 */
public class FreeMarkerViewTests {

	public static final String TEMPLATE_PATH = "classpath*:org/springframework/web/reactive/view/freemarker/";


	private ServerWebExchange exchange;

	private MockServerHttpResponse response;

	private GenericApplicationContext context;

	private Configuration freeMarkerConfig;

	@Before
	public void setUp() throws Exception {
		this.context = new GenericApplicationContext();
		this.context.refresh();

		FreeMarkerConfigurer configurer = new FreeMarkerConfigurer();
		configurer.setPreferFileSystemAccess(false);
		configurer.setTemplateLoaderPath(TEMPLATE_PATH);
		configurer.setResourceLoader(this.context);
		this.freeMarkerConfig = configurer.createConfiguration();

		MockServerHttpRequest request = new MockServerHttpRequest(HttpMethod.GET, "/path");
		this.response = new MockServerHttpResponse();
		WebSessionManager manager = new DefaultWebSessionManager();
		this.exchange = new DefaultServerWebExchange(request, response, manager);
	}


	@Test(expected = IllegalArgumentException.class)
	public void noFreeMarkerConfig() {
		new FreeMarkerView("anythingButNull", null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void noTemplateName() {
		new FreeMarkerView(null, this.freeMarkerConfig);
	}

	@Test
	public void checkResourceExists() throws Exception {
		FreeMarkerView view = new FreeMarkerView("test.ftl", this.freeMarkerConfig);

		assertTrue(view.checkResourceExists(Locale.US));
	}

	@Test
	public void render() throws Exception {
		FreeMarkerView view = new FreeMarkerView("test.ftl", this.freeMarkerConfig);

		ModelMap model = new ExtendedModelMap();
		model.addAttribute("hello", "hi FreeMarker");
		view.render(model, null, this.exchange);

		StepVerifier.create(this.response.getBody())
				.consumeNextWith(buf -> {
					assertEquals("<html><body>hi FreeMarker</body></html>", asString(buf));
				})
				.expectComplete()
				.verify();
	}


	private static String asString(DataBuffer dataBuffer) {
		ByteBuffer byteBuffer = dataBuffer.asByteBuffer();
		final byte[] bytes = new byte[byteBuffer.remaining()];
		byteBuffer.get(bytes);
		return new String(bytes, StandardCharsets.UTF_8);
	}


	@SuppressWarnings("unused")
	private String handle() {
		return null;
	}

}
