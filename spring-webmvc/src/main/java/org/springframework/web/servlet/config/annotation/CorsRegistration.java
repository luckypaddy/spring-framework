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

import org.springframework.web.servlet.cors.CorsConfigSpecification;

import java.util.*;

/**
 * Assists with the creation of an handler mapping and a request handler for
 * CORS requests.
 *
 * @author Sebastien Deleuze
 * @since 4.1
 */
public class CorsRegistration {

	protected CorsConfigSpecification config;

	protected final List<String> paths = new ArrayList<String>();

	/**
	 * Creates an {@link CorsRegistration} instance.
	 */
	public CorsRegistration() {
		this.config = new CorsConfigSpecification();
	}


	/**
	 * Add URL patterns to which the registered interceptor should apply to.
	 * Exact path mapping URIs (such as "/myPath") are supported as well as
	 * Ant-stype path patterns (such as /myPath/**).
	 */
	public CorsRegistration mapping(String... paths) {
		this.paths.addAll(Arrays.asList(paths));
		return this;
	}

	/**
	 * Add allowed origin that will define Access-Control-Allow-Origin response
	 * header values. For example "http://domain1.com", "http://domain2.com" ...
	 */
	public CorsRegistration allowedOrigins(String... allowedOrigins) {
		this.config.getAllowedOrigins().addAll(Arrays.asList(allowedOrigins));
		return this;
	}

	/**
	 * Mandatory for preflight requests handling.
	 * Add allowed methods that will define Access-Control-Allow-Methods response header
	 * values. For example "GET", "POST", "PUT" ...
	 */
	public CorsRegistration allowedMethods(String... allowedMethods) {
		this.config.getAllowedMethods().addAll(Arrays.asList(allowedMethods));
		return this;
	}

	/**
	 * Only relevant for preflight request handling. Add a list of headers that will
	 * define Access-Control-Allow-Methods response.
	 * header values. If a header field name is one of the following, it is not required
	 * to be listed: Cache-Control, Content-Language, Expires, Last-Modified, Pragma.
	 */
	public CorsRegistration allowedHeaders(String... allowedHeaders) {
		this.config.getAllowedHeaders().addAll(Arrays.asList(allowedHeaders));
		return this;
	}

	/**
	 * Only relevant for preflight request handling. Indicates how long (seconds) the
	 * results of a preflight request can be cached in a preflight result cache.
	 */
	public CorsRegistration maxAge(long maxAge) {
		this.config.setMaxAge(maxAge);
		return this;
	}

	/**
	 * Indicates whether the resource supports user credentials.
	 * Set the value of Access-Control-Allow-Credentials response header.
	 */
	public CorsRegistration allowCredentials(boolean allowCredentials) {
		this.config.setAllowCredentials(allowCredentials);
		return this;
	}

	/**
	 * Add a list of headers other than simple response headers that the resource might use
	 * and can be exposed. Simple response headers are: Cache-Control, Content-Language,
	 * Content-Type, Expires, Last-Modified, Pragma.
	 */
	public CorsRegistration exposedHeaders(String... exposedHeaders) {
		this.config.setExposedHeaders(Arrays.asList(exposedHeaders));
		return this;
	}

	/**
	 * Return the underlying configuration.
	 */
	protected CorsConfigSpecification getConfig() {
		return config;
	}

	/**
	 * Return the underlying path patterns.
	 */
	protected List<String> getPaths() {
		return paths;
	}
}
