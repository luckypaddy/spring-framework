/*
 * Copyright 2002-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.cors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.http.HttpMethod;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * A container for CORS configuration that also provides methods to check
 * the actual or requested origin, HTTP methods, and headers.
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 4.2
 * @see <a href="http://www.w3.org/TR/cors/">CORS W3C recommendation</a>
 */
public class CorsConfiguration {

	/**
	 * Wildcard representing <em>all</em> origins, methods, or headers.
	 */
	public static final String ALL = "*";

	private List<String> allowedOrigins;

	private List<String> allowedMethods;

	private List<String> allowedHeaders;

	private List<String> exposedHeaders;

	private Boolean allowCredentials;

	private Long maxAge;

	private Boolean strictFiltering;


	/**
	 * Construct a new, empty {@code CorsConfiguration} instance.
	 */
	public CorsConfiguration() {
	}

	/**
	 * Construct a new {@code CorsConfiguration} instance by copying all
	 * values from the supplied {@code CorsConfiguration}.
	 */
	public CorsConfiguration(CorsConfiguration other) {
		this.allowedOrigins = other.allowedOrigins;
		this.allowedMethods = other.allowedMethods;
		this.allowedHeaders = other.allowedHeaders;
		this.exposedHeaders = other.exposedHeaders;
		this.allowCredentials = other.allowCredentials;
		this.maxAge = other.maxAge;
		this.strictFiltering = other.strictFiltering;
	}

	/**
	 * Combine the supplied {@code CorsConfiguration} with this one.
	 * <p>Properties of this configuration are overridden by any non-null
	 * properties of the supplied one.
	 * @return the combined {@code CorsConfiguration} or {@code this}
	 * configuration if the supplied configuration is {@code null}
	 */
	public CorsConfiguration combine(CorsConfiguration other) {
		if (other == null) {
			return this;
		}
		CorsConfiguration config = new CorsConfiguration(this);
		config.setAllowedOrigins(combine(this.getAllowedOrigins(), other.getAllowedOrigins()));
		config.setAllowedMethods(combine(this.getAllowedMethods(), other.getAllowedMethods()));
		config.setAllowedHeaders(combine(this.getAllowedHeaders(), other.getAllowedHeaders()));
		config.setExposedHeaders(combine(this.getExposedHeaders(), other.getExposedHeaders()));
		Boolean allowCredentials = other.getAllowCredentials();
		if (allowCredentials != null) {
			config.setAllowCredentials(allowCredentials);
		}
		Long maxAge = other.getMaxAge();
		if (maxAge != null) {
			config.setMaxAge(maxAge);
		}
		Boolean strictFiltering = other.getStrictFiltering();
		if (strictFiltering != null) {
			config.setStrictFiltering(strictFiltering);
		}
		return config;
	}

	private List<String> combine(List<String> source, List<String> other) {
		if (other == null) {
			return source;
		}
		if (source == null || source.contains(ALL)) {
			return other;
		}
		List<String> combined = new ArrayList<String>(source);
		combined.addAll(other);
		return combined;
	}

	/**
	 * Set the origins to allow, e.g. {@code "http://domain1.com"}.
	 * <p>The special value {@code "*"} allows all domains.
	 * <p>By default this is not set.
	 */
	public void setAllowedOrigins(List<String> allowedOrigins) {
		this.allowedOrigins = (allowedOrigins == null ? null : new ArrayList<String>(allowedOrigins));
	}

	/**
	 * Add an origin to allow.
	 */
	public void addAllowedOrigin(String origin) {
		if (this.allowedOrigins == null) {
			this.allowedOrigins = new ArrayList<String>();
		}
		this.allowedOrigins.add(origin);
	}

	/**
	 * Return the configured origins to allow, possibly {@code null}.
	 * @see #addAllowedOrigin(String)
	 * @see #setAllowedOrigins(List)
	 */
	public List<String> getAllowedOrigins() {
		return this.allowedOrigins;
	}

	/**
	 * Set the HTTP methods to allow, e.g. {@code "GET"}, {@code "POST"},
	 * {@code "PUT"}, etc.
	 * <p>The special value {@code "*"} allows all methods.
	 * <p>If not set, only {@code "GET"} is allowed.
	 * <p>By default this is not set.
	 */
	public void setAllowedMethods(List<String> allowedMethods) {
		this.allowedMethods = (allowedMethods == null ? null : new ArrayList<String>(allowedMethods));
	}

	/**
	 * Add an HTTP method to allow.
	 */
	public void addAllowedMethod(HttpMethod method) {
		if (method != null) {
			addAllowedMethod(method.name());
		}
	}

	/**
	 * Add an HTTP method to allow.
	 */
	public void addAllowedMethod(String method) {
		if (StringUtils.hasText(method)) {
			if (this.allowedMethods == null) {
				this.allowedMethods = new ArrayList<String>();
			}
			this.allowedMethods.add(method);
		}
	}

	/**
	 * Return the allowed HTTP methods, possibly {@code null} in which case
	 * only {@code "GET"} is allowed.
	 * @see #addAllowedMethod(HttpMethod)
	 * @see #addAllowedMethod(String)
	 * @see #setAllowedMethods(List)
	 */
	public List<String> getAllowedMethods() {
		return this.allowedMethods;
	}

	/**
	 * Set the list of headers that a pre-flight request can list as allowed
	 * for use during an actual request.
	 * <p>The special value {@code "*"} allows actual requests to send any
	 * header.
	 * <p>A header name is not required to be listed if it is one of:
	 * {@code Cache-Control}, {@code Content-Language}, {@code Expires},
	 * {@code Last-Modified}, or {@code Pragma}.
	 * <p>By default this is not set.
	 */
	public void setAllowedHeaders(List<String> allowedHeaders) {
		this.allowedHeaders = (allowedHeaders == null ? null : new ArrayList<String>(allowedHeaders));
	}

	/**
	 * Add an actual request header to allow.
	 */
	public void addAllowedHeader(String allowedHeader) {
		if (this.allowedHeaders == null) {
			this.allowedHeaders = new ArrayList<String>();
		}
		this.allowedHeaders.add(allowedHeader);
	}

	/**
	 * Return the allowed actual request headers, possibly {@code null}.
	 * @see #addAllowedHeader(String)
	 * @see #setAllowedHeaders(List)
	 */
	public List<String> getAllowedHeaders() {
		return this.allowedHeaders;
	}

	/**
	 * Set the list of response headers other than simple headers (i.e.
	 * {@code Cache-Control}, {@code Content-Language}, {@code Content-Type},
	 * {@code Expires}, {@code Last-Modified}, or {@code Pragma}) that an
	 * actual response might have and can be exposed.
	 * <p>Note that {@code "*"} is not a valid exposed header value.
	 * <p>By default this is not set.
	 */
	public void setExposedHeaders(List<String> exposedHeaders) {
		if (exposedHeaders != null && exposedHeaders.contains(ALL)) {
			throw new IllegalArgumentException("'*' is not a valid exposed header value");
		}
		this.exposedHeaders = (exposedHeaders == null ? null : new ArrayList<String>(exposedHeaders));
	}

	/**
	 * Add a response header to expose.
	 * <p>Note that {@code "*"} is not a valid exposed header value.
	 */
	public void addExposedHeader(String exposedHeader) {
		if (ALL.equals(exposedHeader)) {
			throw new IllegalArgumentException("'*' is not a valid exposed header value");
		}
		if (this.exposedHeaders == null) {
			this.exposedHeaders = new ArrayList<String>();
		}
		this.exposedHeaders.add(exposedHeader);
	}

	/**
	 * Return the configured response headers to expose, possibly {@code null}.
	 * @see #addExposedHeader(String)
	 * @see #setExposedHeaders(List)
	 */
	public List<String> getExposedHeaders() {
		return this.exposedHeaders;
	}

	/**
	 * Whether user credentials are supported.
	 * <p>By default this is not set (i.e. user credentials are not supported).
	 */
	public void setAllowCredentials(Boolean allowCredentials) {
		this.allowCredentials = allowCredentials;
	}

	/**
	 * Return the configured {@code allowCredentials} flag, possibly {@code null}.
	 * @see #setAllowCredentials(Boolean)
	 */
	public Boolean getAllowCredentials() {
		return this.allowCredentials;
	}

	/**
	 * Configure how long, in seconds, the response from a pre-flight request
	 * can be cached by clients.
	 * <p>By default this is not set.
	 */
	public void setMaxAge(Long maxAge) {
		this.maxAge = maxAge;
	}

	/**
	 * Return the configured {@code maxAge} value, possibly {@code null}.
	 * @see #setMaxAge(Long)
	 */
	public Long getMaxAge() {
		return maxAge;
	}

	/**
	 * Whether invalid simple/actual requests are rejected {@code true}, or pass through
	 * the CORS processor without any CORS response headers added {@code false}.
	 * <p>If undefined, strict filtering is enabled. Be aware that browsers like Chrome
	 * or Safari include an {@code Origin} header on same-origin
	 * {@code POST}/{@code PUT}/{@code DELETE} requests so these requests will be detected
	 * as cross-origin requests.
	 */
	public void setStrictFiltering(Boolean strictFiltering) {
		this.strictFiltering = strictFiltering;
	}

	/**
	 * Return the configured {@code strictFiltering} flag, possibly {@code null}.
	 * @see #setStrictFiltering(Boolean)
	 */
	public Boolean getStrictFiltering() {
		return strictFiltering;
	}

	/**
	 * Check the origin of the request against the configured allowed origins.
	 * @param requestOrigin the origin to check
	 * @return the origin to use for the response, possibly {@code null} which
	 * means the request origin is not allowed
	 */
	public String checkOrigin(String requestOrigin) {
		if (!StringUtils.hasText(requestOrigin)) {
			return null;
		}
		if (ObjectUtils.isEmpty(this.allowedOrigins)) {
			return null;
		}

		if (this.allowedOrigins.contains(ALL)) {
			if ((this.allowCredentials == null) || !this.allowCredentials.booleanValue()) {
				return ALL;
			} else {
				return requestOrigin;
			}
		}
		for (String allowedOrigin : this.allowedOrigins) {
			if (requestOrigin.equalsIgnoreCase(allowedOrigin)) {
				return requestOrigin;
			}
		}
		return null;
	}

	/**
	 * Check the HTTP request method (or the method from the
	 * {@code Access-Control-Request-Method} header on a pre-flight request)
	 * against the configured allowed methods.
	 * @param requestMethod the HTTP request method to check
	 * @return the list of HTTP methods to list in the response of a pre-flight
	 * request, or {@code null} if the supplied {@code requestMethod} is not allowed
	 */
	public List<HttpMethod> checkHttpMethod(HttpMethod requestMethod) {
		if (requestMethod == null) {
			return null;
		}
		List<String> allowedMethods = (this.allowedMethods == null ? new ArrayList<String>() : this.allowedMethods);
		if (allowedMethods.contains(ALL)) {
			return Arrays.asList(requestMethod);
		}
		if (allowedMethods.isEmpty()) {
			allowedMethods.add(HttpMethod.GET.name());
		}
		List<HttpMethod> result = new ArrayList<HttpMethod>(allowedMethods.size());
		boolean allowed = false;
		for (String method : allowedMethods) {
			if (requestMethod.name().equals(method)) {
				allowed = true;
			}
			result.add(HttpMethod.valueOf(method));
		}
		return (allowed ? result : null);
	}

	/**
	 * Check the supplied request headers (or the headers listed in the
	 * {@code Access-Control-Request-Headers} of a pre-flight request) against
	 * the configured allowed headers.
	 * @param requestHeaders the request headers to check
	 * @return the list of allowed headers to list in the response of a pre-flight
	 * request, or {@code null} if none of the supplied request headers is allowed
	 */
	public List<String> checkHeaders(List<String> requestHeaders) {
		if (requestHeaders == null) {
			return null;
		}
		if (requestHeaders.isEmpty()) {
			return Collections.emptyList();
		}
		if (ObjectUtils.isEmpty(this.allowedHeaders)) {
			return null;
		}

		boolean allowAnyHeader = this.allowedHeaders.contains(ALL);
		List<String> result = new ArrayList<String>();
		for (String requestHeader : requestHeaders) {
			if (StringUtils.hasText(requestHeader)) {
				requestHeader = requestHeader.trim();
				for (String allowedHeader : this.allowedHeaders) {
					if (allowAnyHeader || requestHeader.equalsIgnoreCase(allowedHeader)) {
						result.add(requestHeader);
						break;
					}
				}
			}
		}
		return (result.isEmpty() ? null : result);
	}

}
