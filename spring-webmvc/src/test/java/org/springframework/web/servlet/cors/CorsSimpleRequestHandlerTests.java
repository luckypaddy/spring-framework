/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.web.servlet.cors;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.cors.CorsConfigSpecification;
import org.springframework.web.servlet.cors.CorsHandler;

import javax.servlet.http.HttpServletResponse;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Test fixture with a CorsSimpleRequestHandlerInterceptor.
 *
 * @author Sebastien Deleuze
 * @since 4.1
 */
public class CorsSimpleRequestHandlerTests {

	private MockHttpServletRequest request;
	private MockHttpServletResponse response;
	private CorsConfigSpecification config;
	private CorsHandler handler;

	@Before
	public void setup() {
		this.request = new MockHttpServletRequest();
		this.request.setMethod(RequestMethod.GET.name());
		this.request.setRequestURI("/test.html");
		this.request.setRemoteHost("domain1.com");
		this.response = new MockHttpServletResponse();
		this.response.setStatus(HttpServletResponse.SC_OK);
		this.config = new CorsConfigSpecification();
		this.handler = new CorsHandler();
		this.handler.addCorsMapping("/**", this.config);
	}

	@Test
	public void testWithoutOriginHeader() throws Exception {
		this.handler.preHandle(request, response, new Object());
		assertFalse(response.containsHeader(CorsHandler.HEADER_ACCESS_CONTROL_ALLOW_ORIGIN));
		assertEquals(HttpServletResponse.SC_OK, response.getStatus());
	}

	@Test
	public void testWithOriginHeader() throws Exception {
		this.request.addHeader(CorsHandler.HEADER_ORIGIN, "http://domain2.com/test.html");
		this.handler.preHandle(request, response, new Object());
		assertFalse(response.containsHeader(CorsHandler.HEADER_ACCESS_CONTROL_ALLOW_ORIGIN));
		assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
	}

	@Test
	public void testWithOriginHeaderAndAllowedOrigin() throws Exception {
		this.request.addHeader(CorsHandler.HEADER_ORIGIN, "http://domain2.com/test.html");
		this.config.addAllowedOrigin("*");
		this.handler.preHandle(request, response, new Object());
		assertTrue(response.containsHeader(CorsHandler.HEADER_ACCESS_CONTROL_ALLOW_ORIGIN));
		assertEquals("*", response.getHeader(CorsHandler.HEADER_ACCESS_CONTROL_ALLOW_ORIGIN));
		assertFalse(response.containsHeader(CorsHandler.HEADER_ACCESS_CONTROL_MAX_AGE));
		assertFalse(response.containsHeader(CorsHandler.HEADER_ACCESS_CONTROL_EXPOSE_HEADERS));
		assertEquals(HttpServletResponse.SC_OK, response.getStatus());
	}

	@Test
	public void testCrendentials() throws Exception {
		this.request.addHeader(CorsHandler.HEADER_ORIGIN, "http://domain2.com/test.html");
		this.config.addAllowedOrigin("http://domain2.com/home.html");
		this.config.addAllowedOrigin("http://domain2.com/test.html");
		this.config.addAllowedOrigin("http://domain2.com/logout.html");
		this.config.setAllowCredentials(true);
		this.handler.preHandle(request, response, new Object());
		assertTrue(response.containsHeader(CorsHandler.HEADER_ACCESS_CONTROL_ALLOW_ORIGIN));
		assertEquals("http://domain2.com/test.html", response.getHeader(CorsHandler.HEADER_ACCESS_CONTROL_ALLOW_ORIGIN));
		assertTrue(response.containsHeader(CorsHandler.HEADER_ACCESS_CONTROL_ALLOW_CREDENTIALS));
		assertEquals("true", response.getHeader(CorsHandler.HEADER_ACCESS_CONTROL_ALLOW_CREDENTIALS));
		assertEquals(HttpServletResponse.SC_OK, response.getStatus());
	}

	@Test
	public void testCrendentialsWithOriginWildcard() throws Exception {
		this.request.addHeader(CorsHandler.HEADER_ORIGIN, "http://domain2.com/test.html");
		this.config.addAllowedOrigin("*");
		this.config.setAllowCredentials(true);
		this.handler.preHandle(request, response, new Object());
		assertTrue(response.containsHeader(CorsHandler.HEADER_ACCESS_CONTROL_ALLOW_ORIGIN));
		assertEquals("http://domain2.com/test.html", response.getHeader(CorsHandler.HEADER_ACCESS_CONTROL_ALLOW_ORIGIN));
		assertTrue(response.containsHeader(CorsHandler.HEADER_ACCESS_CONTROL_ALLOW_CREDENTIALS));
		assertEquals("true", response.getHeader(CorsHandler.HEADER_ACCESS_CONTROL_ALLOW_CREDENTIALS));
		assertEquals(HttpServletResponse.SC_OK, response.getStatus());
	}

	@Test
	public void testCaseInsensitiveOriginMatch() throws Exception {
		this.request.addHeader(CorsHandler.HEADER_ORIGIN, "http://domain2.com/test.html");
		this.config.addAllowedOrigin("http://domain2.com/TEST.html");
		this.handler.preHandle(request, response, new Object());
		assertTrue(response.containsHeader(CorsHandler.HEADER_ACCESS_CONTROL_ALLOW_ORIGIN));
		assertEquals(HttpServletResponse.SC_OK, response.getStatus());
	}

	@Test
	 public void testExposedHeaders() throws Exception {
		this.request.addHeader(CorsHandler.HEADER_ORIGIN, "http://domain2.com/test.html");
		List<String> exposedHeaders = new ArrayList<String>();
		exposedHeaders.add("header1");
		exposedHeaders.add("header2");
		this.config.addAllowedOrigin("http://domain2.com/test.html");
		this.config.setExposedHeaders(exposedHeaders);
		this.handler.preHandle(request, response, new Object());
		assertTrue(response.containsHeader(CorsHandler.HEADER_ACCESS_CONTROL_ALLOW_ORIGIN));
		assertEquals("http://domain2.com/test.html", response.getHeader(CorsHandler.HEADER_ACCESS_CONTROL_ALLOW_ORIGIN));
		assertTrue(response.containsHeader(CorsHandler.HEADER_ACCESS_CONTROL_EXPOSE_HEADERS));
		assertTrue(response.getHeader(CorsHandler.HEADER_ACCESS_CONTROL_EXPOSE_HEADERS).contains("header1"));
		assertTrue(response.getHeader(CorsHandler.HEADER_ACCESS_CONTROL_EXPOSE_HEADERS).contains("header2"));
		assertEquals(HttpServletResponse.SC_OK, response.getStatus());
	}

}
