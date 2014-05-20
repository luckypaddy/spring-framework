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
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test fixture with a CorsPreflightRequestHandler.
 *
 * @author Sebastien Deleuze
 * @since 4.1
 */
public class CorsPreflightRequestHandlerTests {

	private MockHttpServletRequest request;
	private MockHttpServletResponse response;
	private CorsConfigSpecification config;
	private CorsHandler handler;

	@Before
	public void setup() {
		this.request = new MockHttpServletRequest();
		this.request.setMethod(RequestMethod.OPTIONS.name());
		this.request.setRequestURI("/test.html");
		this.request.setRemoteHost("domain1.com");
		this.response = new MockHttpServletResponse();
		this.response.setStatus(HttpServletResponse.SC_OK);
		this.config = new CorsConfigSpecification();
		this.handler = new CorsHandler();
		this.handler.addCorsMapping("/**", this.config);
	}

	@Test
	public void testNoHeader() throws Exception {
		this.request.addHeader(CorsHandler.HEADER_ORIGIN, "http://domain2.com/test.html");
		this.request.addHeader(CorsHandler.HEADER_ACCESS_CONTROL_REQUEST_METHOD, "GET");
		this.config.addAllowedOrigin("*");
		this.config.addAllowedMethod("GET");
		this.handler.handleRequest(request, response);
		assertEquals(HttpServletResponse.SC_OK, response.getStatus());
	}

	@Test
	public void testWrongMethod() throws Exception {
		this.request.addHeader(CorsHandler.HEADER_ORIGIN, "http://domain2.com/test.html");
		this.request.addHeader(CorsHandler.HEADER_ACCESS_CONTROL_REQUEST_METHOD, "GET");
		this.config.addAllowedOrigin("*");
		this.config.addAllowedMethod("GET");
		this.request.setMethod(RequestMethod.GET.name());
		this.handler.handleRequest(request, response);
		assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
	}

	@Test
	public void testWrongAllowedMethod() throws Exception {
		this.request.addHeader(CorsHandler.HEADER_ORIGIN, "http://domain2.com/test.html");
		this.request.addHeader(CorsHandler.HEADER_ACCESS_CONTROL_REQUEST_METHOD, "GET");
		this.config.addAllowedOrigin("*");
		this.config.addAllowedMethod("POST");
		this.handler.handleRequest(request, response);
		assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
	}

	@Test
	public void testWildcardAllowedMethod() throws Exception {
		this.request.addHeader(CorsHandler.HEADER_ORIGIN, "http://domain2.com/test.html");
		this.request.addHeader(CorsHandler.HEADER_ACCESS_CONTROL_REQUEST_METHOD, "GET");
		this.config.addAllowedOrigin("*");
		this.config.addAllowedMethod("*");
		this.handler.handleRequest(request, response);
		assertEquals(HttpServletResponse.SC_OK, response.getStatus());
		assertEquals("GET", response.getHeader(CorsHandler.HEADER_ACCESS_CONTROL_ALLOW_METHODS));
	}

	@Test
	public void testMatchedAllowedMethod() throws Exception {
		this.request.addHeader(CorsHandler.HEADER_ORIGIN, "http://domain2.com/test.html");
		this.request.addHeader(CorsHandler.HEADER_ACCESS_CONTROL_REQUEST_METHOD, "GET");
		this.config.addAllowedOrigin("*");
		this.config.addAllowedMethod("GET");
		this.handler.handleRequest(request, response);
		assertEquals(HttpServletResponse.SC_OK, response.getStatus());
		assertEquals("GET", response.getHeader(CorsHandler.HEADER_ACCESS_CONTROL_ALLOW_METHODS));
	}

	@Test
	public void testWithoutOriginHeader() throws Exception {
		this.request.addHeader(CorsHandler.HEADER_ACCESS_CONTROL_REQUEST_METHOD, "GET");
		this.handler.handleRequest(request, response);
		assertFalse(response.containsHeader("Access-Control-Allow-Origin"));
		assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
	}

	@Test
	public void testWithOriginButWithoutOtherHeaders() throws Exception {
		this.request.addHeader(CorsHandler.HEADER_ORIGIN, "http://domain2.com/test.html");
		this.handler.handleRequest(request, response);
		assertFalse(response.containsHeader("Access-Control-Allow-Origin"));
		assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
	}

	@Test
	public void testWithoutRequestMethod() throws Exception {
		this.request.addHeader(CorsHandler.HEADER_ORIGIN, "http://domain2.com/test.html");
		this.request.addHeader(CorsHandler.HEADER_ACCESS_CONTROL_REQUEST_HEADERS, "Header1");
		this.handler.handleRequest(request, response);
		assertFalse(response.containsHeader(CorsHandler.HEADER_ACCESS_CONTROL_ALLOW_ORIGIN));
		assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
	}

	@Test
	public void testWithRequestAndMethodHeaderButNoConfig() throws Exception {
		this.request.addHeader(CorsHandler.HEADER_ORIGIN, "http://domain2.com/test.html");
		this.request.addHeader(CorsHandler.HEADER_ACCESS_CONTROL_REQUEST_HEADERS, "Header1");
		this.request.addHeader(CorsHandler.HEADER_ACCESS_CONTROL_REQUEST_METHOD, "GET");
		this.handler.handleRequest(request, response);
		assertFalse(response.containsHeader(CorsHandler.HEADER_ACCESS_CONTROL_ALLOW_ORIGIN));
		assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
	}

	@Test
	public void testValidRequestAndConfig() throws Exception {
		this.request.addHeader(CorsHandler.HEADER_ORIGIN, "http://domain2.com/test.html");
		this.request.addHeader(CorsHandler.HEADER_ACCESS_CONTROL_REQUEST_HEADERS, "Header1");
		this.request.addHeader(CorsHandler.HEADER_ACCESS_CONTROL_REQUEST_METHOD, "GET");
		this.config.addAllowedOrigin("*");
		this.config.addAllowedMethod("GET");
		this.config.addAllowedMethod("PUT");
		this.config.addAllowedHeader("header1");
		this.config.addAllowedHeader("header2");
		this.handler.handleRequest(request, response);
		assertTrue(response.containsHeader(CorsHandler.HEADER_ACCESS_CONTROL_ALLOW_ORIGIN));
		assertEquals( "*", response.getHeader(CorsHandler.HEADER_ACCESS_CONTROL_ALLOW_ORIGIN));
		assertTrue(response.containsHeader(CorsHandler.HEADER_ACCESS_CONTROL_ALLOW_METHODS));
		assertEquals("GET, PUT", response.getHeader(CorsHandler.HEADER_ACCESS_CONTROL_ALLOW_METHODS));
		assertTrue(response.containsHeader(CorsHandler.HEADER_ACCESS_CONTROL_MAX_AGE));
		assertEquals(String.valueOf(TimeUnit.DAYS.toSeconds(365)), response.getHeader(CorsHandler.HEADER_ACCESS_CONTROL_MAX_AGE));
		assertEquals(HttpServletResponse.SC_OK, response.getStatus());
	}

	@Test
	public void testCrendentials() throws Exception {
		this.request.addHeader(CorsHandler.HEADER_ORIGIN, "http://domain2.com/test.html");
		this.request.addHeader(CorsHandler.HEADER_ACCESS_CONTROL_REQUEST_HEADERS, "Header1");
		this.request.addHeader(CorsHandler.HEADER_ACCESS_CONTROL_REQUEST_METHOD, "GET");
		this.config.addAllowedOrigin("http://domain2.com/home.html");
		this.config.addAllowedOrigin("http://domain2.com/test.html");
		this.config.addAllowedOrigin("http://domain2.com/logout.html");
		this.config.addAllowedMethod("GET");
		this.config.addAllowedHeader("Header1");
		this.config.setAllowCredentials(true);
		this.handler.handleRequest(request, response);
		assertTrue(response.containsHeader("Access-Control-Allow-Origin"));
		assertEquals("http://domain2.com/test.html", response.getHeader(CorsHandler.HEADER_ACCESS_CONTROL_ALLOW_ORIGIN));
		assertTrue(response.containsHeader(CorsHandler.HEADER_ACCESS_CONTROL_ALLOW_CREDENTIALS));
		assertEquals("true", response.getHeader(CorsHandler.HEADER_ACCESS_CONTROL_ALLOW_CREDENTIALS));
		assertEquals(HttpServletResponse.SC_OK, response.getStatus());
	}

	@Test
	public void testCrendentialsWithOriginWildcard() throws Exception {
		this.request.addHeader(CorsHandler.HEADER_ORIGIN, "http://domain2.com/test.html");
		this.request.addHeader(CorsHandler.HEADER_ACCESS_CONTROL_REQUEST_HEADERS, "Header1");
		this.request.addHeader(CorsHandler.HEADER_ACCESS_CONTROL_REQUEST_METHOD, "GET");
		this.config.addAllowedOrigin("http://domain2.com/home.html");
		this.config.addAllowedOrigin("*");
		this.config.addAllowedOrigin("http://domain2.com/logout.html");
		this.config.addAllowedMethod("GET");
		this.config.addAllowedHeader("Header1");
		this.config.setAllowCredentials(true);
		this.handler.handleRequest(request, response);
		assertTrue(response.containsHeader(CorsHandler.HEADER_ACCESS_CONTROL_ALLOW_ORIGIN));
		assertEquals("http://domain2.com/test.html", response.getHeader(CorsHandler.HEADER_ACCESS_CONTROL_ALLOW_ORIGIN));
		assertEquals(HttpServletResponse.SC_OK, response.getStatus());
	}

	@Test
	public void testAllowedHeaders() throws Exception {
		this.request.addHeader(CorsHandler.HEADER_ORIGIN, "http://domain2.com/test.html");
		this.request.addHeader(CorsHandler.HEADER_ACCESS_CONTROL_REQUEST_HEADERS, "Header1, Header2, Header3");
		this.request.addHeader(CorsHandler.HEADER_ACCESS_CONTROL_REQUEST_METHOD, "GET");
		List<String> allowedHeaders = new ArrayList<String>();
		allowedHeaders.add("Header1");
		allowedHeaders.add("Header2");
		this.config.addAllowedOrigin("http://domain2.com/test.html");
		this.config.addAllowedMethod("GET");
		this.config.setAllowedHeaders(allowedHeaders);
		this.handler.handleRequest(request, response);
		assertTrue(response.containsHeader(CorsHandler.HEADER_ACCESS_CONTROL_ALLOW_ORIGIN));
		assertTrue(response.containsHeader(CorsHandler.HEADER_ACCESS_CONTROL_ALLOW_HEADERS));
		assertTrue(response.getHeader(CorsHandler.HEADER_ACCESS_CONTROL_ALLOW_HEADERS).contains("Header1"));
		assertTrue(response.getHeader(CorsHandler.HEADER_ACCESS_CONTROL_ALLOW_HEADERS).contains("Header2"));
		assertFalse(response.getHeader(CorsHandler.HEADER_ACCESS_CONTROL_ALLOW_HEADERS).contains("Header3"));
		assertEquals(HttpServletResponse.SC_OK, response.getStatus());
	}

}
