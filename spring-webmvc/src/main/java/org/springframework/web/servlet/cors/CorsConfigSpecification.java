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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Configuration for CORS preflight request handling.
 *
 * @author Sebastien Deleuze
 * @since 4.1
 */
public class CorsConfigSpecification {

	private static final long ONE_YEAR = TimeUnit.DAYS.toSeconds(365);

	private List<String> allowedOrigins;
	private boolean allowCredentials;
	private List<String> exposedHeaders;
	private List<String> allowedMethods;
	private List<String> allowedHeaders;
	private Long maxAge;

	public CorsConfigSpecification() {
		super();
		this.allowedOrigins = new ArrayList<String>();
		this.exposedHeaders = new ArrayList<String>();
		this.allowedMethods = new ArrayList<String>();
		this.allowedHeaders = new ArrayList<String>();
		this.maxAge = ONE_YEAR;
	}

	/**
	 * @see #setAllowedOrigins(java.util.List)
	 */
	public List<String> getAllowedOrigins() {
		return allowedOrigins;
	}

	/**
	 * Set allowed origins that will define Access-Control-Allow-Origin response
	 * header values. For example "http://domain1.com", "http://domain2.com" ...
	 */
	public void setAllowedOrigins(List<String> allowedOrigins) {
		this.allowedOrigins = allowedOrigins;
	}

	/**
	 * @see #setAllowedOrigins(java.util.List)
	 */
	public void addAllowedOrigin(String allowedOrigin) {
		this.allowedOrigins.add(allowedOrigin);
	}

	/**
	 * @see #setAllowCredentials(boolean)
	 */
	public boolean isAllowCredentials() {
		return allowCredentials;
	}

	/**
	 * Indicates whether the resource supports user credentials.
	 * Set the value of Access-Control-Allow-Credentials response header.
	 */
	public void setAllowCredentials(boolean allowCredentials) {
		this.allowCredentials = allowCredentials;
	}

	/**
	 * @see #setExposedHeaders(java.util.List)
	 */
	public List<String> getExposedHeaders() {
		return exposedHeaders;
	}

	/**
	 * Set a list of headers other than simple response headers that the resource might use
	 * and can be exposed. Simple response headers are: Cache-Control, Content-Language,
	 * Content-Type, Expires, Last-Modified, Pragma.
	 */
	public void setExposedHeaders(List<String> exposedHeaders) {
		this.exposedHeaders = exposedHeaders;
	}

	/**
	 * @see #setExposedHeaders(java.util.List)
	 */
	public void addExposedHeader(String exposedHeaders) {
		this.exposedHeaders.addAll(Arrays.asList(exposedHeaders));
	}

	/**
	 * @see #setAllowedMethods(java.util.List)
	 */
	public List<String> getAllowedMethods() {
		return allowedMethods;
	}

	/**
	 * Set allow methods that will define Access-Control-Allow-Methods response header
	 * values. For example "GET", "POST", "PUT" ...
	 */
	public void setAllowedMethods(List<String> allowedMethods) {
		this.allowedMethods = allowedMethods;
	}

	/**
	 * @see #setAllowedMethods(java.util.List)
	 */
	public void addAllowedMethod(String allowedMethod) {
		this.allowedMethods.add(allowedMethod);
	}

	/**
	 * @see #setAllowedHeaders(java.util.List)
	 */
	public List<String> getAllowedHeaders() {
		return allowedHeaders;
	}

	/**
	 * Set a list of headers that will define Access-Control-Allow-Methods response
	 * header values. If a header field name is one of the following, it is not required
	 * to be listed: Cache-Control, Content-Language, Expires, Last-Modified, Pragma.
	 */
	public void setAllowedHeaders(List<String> allowedHeaders) {
		this.allowedHeaders = allowedHeaders;
	}

	/**
	 * @see #setAllowedHeaders(java.util.List)
	 */
	public void addAllowedHeader(String allowedHeader) {
		this.allowedHeaders.add(allowedHeader);
	}

	/**
	 * @see #setMaxAge(Long)
	 */
	public Long getMaxAge() {
		return maxAge;
	}

	/**
	 * Indicates how long (seconds) the results of a preflight request can be cached
	 * in a preflight result cache.
	 */
	public void setMaxAge(Long maxAge) {
		this.maxAge = maxAge;
	}
}
