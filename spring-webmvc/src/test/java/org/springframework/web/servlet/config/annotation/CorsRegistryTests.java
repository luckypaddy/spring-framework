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

package org.springframework.web.servlet.config.annotation;

import org.junit.Before;
import org.junit.Test;
import org.springframework.web.servlet.cors.CorsConfigSpecification;
import org.springframework.web.servlet.cors.CorsHandler;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

/**
 * Test fixture with a {@link CorsRegistryTests}
 * @author Sebastien Deleuze
 */
public class CorsRegistryTests {

	private CorsRegistry registry;

	@Before
	public void setUp() {
		registry = new CorsRegistry();
	}

	@Test
	public void defaultSimpleRequestInterceptor() {
		registry.addCorsHandlerMapping();
		CorsHandler corsHandler = registry.getCorsHandler();
		assertEquals(-1, corsHandler.getOrder());
		assertEquals(1, corsHandler.getUrlConfigMap().size());
		CorsConfigSpecification config = corsHandler.getUrlConfigMap().get("/**");
		assertNotNull(config.getAllowedOrigins());
		assertEquals(1, config.getAllowedOrigins().size());
		assertEquals("*", config.getAllowedOrigins().get(0));
		assertTrue(config.getExposedHeaders().isEmpty());
		assertFalse(config.isAllowCredentials());
	}

	@Test
	public void sampleSimpleRequestInterceptor() {
		registry.addCorsHandlerMapping().mapping("/path/**").allowCredentials(true)
				.allowedOrigins("http://domain1.com", "http://domain2.com").exposedHeaders("Header1", "Header2");
		CorsHandler corsHandler = registry.getCorsHandler();
		assertEquals(-1, corsHandler.getOrder());
		assertEquals(1, corsHandler.getUrlConfigMap().size());
		CorsConfigSpecification config = corsHandler.getUrlConfigMap().get("/path/**");
		assertNotNull(config.getAllowedOrigins());
		assertEquals(2, config.getAllowedOrigins().size());
		assertEquals("http://domain1.com", config.getAllowedOrigins().get(0));
		assertEquals("http://domain2.com", config.getAllowedOrigins().get(1));
		assertEquals(2, config.getExposedHeaders().size());
		assertEquals("Header1", config.getExposedHeaders().get(0));
		assertEquals("Header2", config.getExposedHeaders().get(1));
		assertTrue(config.isAllowCredentials());
	}


	@Test
	public void defaultPreflightRequestHandler() {
		registry.addCorsHandlerMapping();
		CorsHandler corsHandler = registry.getCorsHandler();
		assertEquals(-1, corsHandler.getOrder());
		assertEquals(1, corsHandler.getUrlConfigMap().size());
		assertEquals(CorsConfigSpecification.class, corsHandler.getUrlConfigMap().get("/**").getClass());
		CorsConfigSpecification config = corsHandler.getUrlConfigMap().get("/**");
		assertNotNull(config.getAllowedOrigins());
		assertEquals(1, config.getAllowedOrigins().size());
		assertEquals("*", config.getAllowedOrigins().get(0));
		assertTrue(config.getAllowedHeaders().isEmpty());
		assertFalse(config.isAllowCredentials());
		assertEquals(31536000, config.getMaxAge().longValue());
	}

	@Test
	public void samplePreflightRequestHandler() {
		registry.addCorsHandlerMapping().mapping("/path/**").allowCredentials(true)
				.allowedOrigins("http://domain1.com", "http://domain2.com").allowedHeaders("Header1", "Header2").maxAge(100);
		CorsHandler corsHandler = registry.getCorsHandler();
		assertEquals(-1, corsHandler.getOrder());
		assertEquals(1, corsHandler.getUrlConfigMap().size());
		assertNull(corsHandler.getUrlConfigMap().get("/test"));
		assertEquals(CorsConfigSpecification.class, corsHandler.getUrlConfigMap().get("/path/**").getClass());
		CorsConfigSpecification config = corsHandler.getUrlConfigMap().get("/path/**");
		assertNotNull(config.getAllowedOrigins());
		assertEquals(2, config.getAllowedOrigins().size());
		assertEquals("http://domain1.com", config.getAllowedOrigins().get(0));
		assertEquals("http://domain2.com", config.getAllowedOrigins().get(1));
		assertEquals(2, config.getAllowedHeaders().size());
		assertEquals("Header1", config.getAllowedHeaders().get(0));
		assertEquals("Header2", config.getAllowedHeaders().get(1));
		assertTrue(config.isAllowCredentials());
		assertEquals(100, config.getMaxAge().longValue());
	}

}
