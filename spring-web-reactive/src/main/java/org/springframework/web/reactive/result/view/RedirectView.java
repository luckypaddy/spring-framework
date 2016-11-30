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

package org.springframework.web.reactive.result.view;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import reactor.core.publisher.Mono;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

/**
 * View that redirects to an absolute or context relative URL. The URL may be a URI
 * template in which case the URI template variables will be replaced with values
 * available in the model.
 *
 * <p>A URL for this view is supposed to be a HTTP redirect which does the redirect via
 * sending an {@link HttpStatus#SEE_OTHER} code. If HTTP 1.0 compatibility is needed,
 * {@link HttpStatus#FOUND} code can be set via {@link #setStatusCode(HttpStatus)}.
 *
 * <p>Note that the default value for the "contextRelative" flag is true.
 * With the flag on, URLs starting with "/" are considered relative to the web application
 * context path, while with this flag off they are considered relative to the web server
 * root.
 *
 * @author Sebastien Deleuze
 * @see #setContextRelative
 * @since 5.0
 */
public class RedirectView extends AbstractUrlBasedView {

	private static final Pattern URI_TEMPLATE_VARIABLE_PATTERN = Pattern.compile("\\{([^/]+?)\\}");


	private boolean contextRelative = true;

	private HttpStatus statusCode = HttpStatus.SEE_OTHER;

	private boolean propagateQuery = false;

	private String[] hosts;


	/**
	 * Create a new {@code RedirectView} with the given redirect URL.
	 *
	 * @see #movedPermanently(String)
	 * @see #found(String)
	 * @see #seeOther(String)
	 */
	public RedirectView(String redirectUrl, HttpStatus statusCode) {
		super(redirectUrl);
		this.statusCode = statusCode;
	}


	/**
	 * Create a {@code RedirectView} with {@link HttpStatus#SEE_OTHER} status code which is
	 * the default temporary redirect status code to use for HTTP 1.1 clients.
	 */
	public static RedirectView seeOther(String redirectUrl) {
		return new RedirectView(redirectUrl, HttpStatus.SEE_OTHER);
	}

	/**
	 * Create a {@code RedirectView} with {@link HttpStatus#MOVED_PERMANENTLY} status code
	 * which indicates that the target resource has been assigned a new permanent URI.
	 */
	public static RedirectView movedPermanently(String redirectUrl) {
		return new RedirectView(redirectUrl, HttpStatus.MOVED_PERMANENTLY);
	}

	/**
	 * Create a {@code RedirectView} with {@link HttpStatus#FOUND} status code which is
	 * the default temporary redirect status code to use for HTTP 1.0 clients.
	 * @see #seeOther(String)
	 */
	public static RedirectView found(String redirectUrl) {
		return new RedirectView(redirectUrl, HttpStatus.FOUND);
	}


	/**
	 * Set whether to interpret a given URL that starts with a slash ("/")
	 * as relative to the current context path ({@code true}, the default) or to
	 * the web server root ({@code false}).
	 */
	public void setContextRelative(boolean contextRelative) {
		this.contextRelative = contextRelative;
	}

	/**
	 * Return whether to interpret a given URL that starts with a slash ("/")
	 * as relative to the current context path ("true") or to the web server
	 * root ("false").
	 * @return
	 */
	public boolean isContextRelative() {
		return contextRelative;
	}

	/**
	 * Set a customized redirect status code to be used for a redirect. Default is
	 * {@link HttpStatus#SEE_OTHER} which is the correct code for HTTP 1.1
	 * clients. This setter can be used to configure {@link HttpStatus#FOUND}
	 * if HTTP 1.0 clients need to be supported, or any other {@literal 3xx}
	 * status code.
	 */
	public void setStatusCode(HttpStatus statusCode) {
		Assert.notNull(statusCode);
		this.statusCode = statusCode;
	}

	/**
	 * Get the redirect status code.
	 */
	public HttpStatus getStatusCode() {
		return statusCode;
	}

	/**
	 * Set whether to append the query string of the current URL to the redirected URL
	 * ({@code true}) or not ({@code false}, the default).
	 */
	public void setPropagateQuery(boolean propagateQuery) {
		this.propagateQuery = propagateQuery;
	}

	/**
	 * Return whether the query string of the current URL is appended to the redirected URL
	 * ({@code true}) or not ({@code false}).
	 */
	public boolean isPropagateQuery() {
		return propagateQuery;
	}

	/**
	 * Configure one or more hosts associated with the application.
	 * All other hosts will be considered external hosts.
	 * <p>In effect, this property provides a way turn off encoding via
	 * {@link javax.servlet.http.HttpServletResponse#encodeRedirectURL} for URLs that have a
	 * host and that host is not listed as a known host when using a Servlet based engine.
	 * <p>If not set (the default) all URLs are encoded through the response.
	 * @param hosts one or more application hosts
	 */
	public void setHosts(String... hosts) {
		this.hosts = hosts;
	}

	/**
	 * Return the configured application hosts.
	 */
	public String[] getHosts() {
		return this.hosts;
	}

	/**
	 * Convert model to request parameters and redirect to the given URL.
	 * @see #sendRedirect
	 */
	@Override
	protected Mono<Void> renderInternal(Map<String, Object> model, MediaType contentType,
			ServerWebExchange exchange) {
		String targetUrl = createTargetUrl(model, exchange);
		return sendRedirect(exchange, targetUrl);
	}

	/**
	 * Create the target URL by checking if the redirect string is a URI template first,
	 * expanding it with the given model, and then optionally appending simple type model
	 * attributes as query String parameters.
	 */
	protected final String createTargetUrl(Map<String, Object> model, ServerWebExchange exchange) {

		ServerHttpRequest request = exchange.getRequest();
		// Prepare target URL.
		StringBuilder targetUrl = new StringBuilder();
		if (this.contextRelative && getUrl().startsWith("/")) {
			// Do not apply context path to relative URLs.
			targetUrl.append(request.getContextPath());
		}
		targetUrl.append(getUrl());

		Charset charset = this.getDefaultCharset();
		if (StringUtils.hasText(targetUrl)) {
			Map<String, String> variables = getCurrentRequestUriVariables(exchange);
			targetUrl = replaceUriTemplateVariables(targetUrl.toString(), model, variables, charset);
		}
		if (this.propagateQuery) {
		 	appendCurrentQueryParams(targetUrl, request);
		}

		return targetUrl.toString();
	}

	/**
	 * Replace URI template variables in the target URL with encoded model
	 * attributes or URI variables from the current request. Model attributes
	 * referenced in the URL are removed from the model.
	 * @param targetUrl the redirect URL
	 * @param model Map that contains model attributes
	 * @param currentUriVariables current request URI variables to use
	 * @param charset the charset to use
	 */
	protected StringBuilder replaceUriTemplateVariables(String targetUrl,
			Map<String, Object> model, Map<String, String> currentUriVariables, Charset charset) {

		StringBuilder result = new StringBuilder();
		Matcher matcher = URI_TEMPLATE_VARIABLE_PATTERN.matcher(targetUrl);
		int endLastMatch = 0;
		while (matcher.find()) {
			String name = matcher.group(1);
			Object value = (model.containsKey(name) ? model.remove(name) : currentUriVariables.get(name));
			if (value == null) {
				throw new IllegalArgumentException("Model has no value for key '" + name + "'");
			}
			result.append(targetUrl.substring(endLastMatch, matcher.start()));
			try {
				result.append(UriUtils.encodePathSegment(value.toString(), charset.name()));
			}
			catch (UnsupportedEncodingException ex) {
				throw new IllegalStateException(ex);
			}
			endLastMatch = matcher.end();
		}
		result.append(targetUrl.substring(endLastMatch, targetUrl.length()));
		return result;
	}

	@SuppressWarnings("unchecked")
	private Map<String, String> getCurrentRequestUriVariables(ServerWebExchange exchange) {
		String name = HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE;
		return (Map<String, String>) exchange.getAttribute(name).orElse(Collections.emptyMap());
	}

	/**
	 * Append the query string of the current request to the target redirect URL.
	 * @param targetUrl the StringBuilder to append the properties to
	 * @param request the current request
	 */
	protected void appendCurrentQueryParams(StringBuilder targetUrl, ServerHttpRequest request) {
		String query = request.getURI().getQuery();
		if (StringUtils.hasText(query)) {
			// Extract anchor fragment, if any.
			String fragment = null;
			int anchorIndex = targetUrl.indexOf("#");
			if (anchorIndex > -1) {
				fragment = targetUrl.substring(anchorIndex);
				targetUrl.delete(anchorIndex, targetUrl.length());
			}

			if (targetUrl.toString().indexOf('?') < 0) {
				targetUrl.append('?').append(query);
			}
			else {
				targetUrl.append('&').append(query);
			}
			// Append anchor fragment, if any, to end of URL.
			if (fragment != null) {
				targetUrl.append(fragment);
			}
		}
	}

	/**
	 * Send a redirect back to the HTTP client
	 * @param exchange current HTTP exchange
	 * @param targetUrl the target URL to redirect to
	 */
	protected Mono<Void> sendRedirect(ServerWebExchange exchange, String targetUrl) {
		ServerHttpResponse response = exchange.getResponse();
		// TODO Support encoding redirect URL as ServerHttpResponse level when SPR-14529 will be fixed
		response.getHeaders().setLocation(URI.create(targetUrl));
		response.setStatusCode(this.statusCode);
		return Mono.empty();
	}

	/**
	 * Whether the given targetUrl has a host that is a "foreign" system in which
	 * case {@link javax.servlet.http.HttpServletResponse#encodeRedirectURL} will not be applied.
	 * This method returns {@code true} if the {@link #setHosts(String[])}
	 * property is configured and the target URL has a host that does not match.
	 * @param targetUrl the target redirect URL
	 * @return {@code true} the target URL has a remote host, {@code false} if it
	 * the URL does not have a host or the "host" property is not configured.
	 */
	protected boolean isRemoteHost(String targetUrl) {
		if (ObjectUtils.isEmpty(this.hosts)) {
			return false;
		}
		String targetHost = UriComponentsBuilder.fromUriString(targetUrl).build().getHost();
		if (StringUtils.isEmpty(targetHost)) {
			return false;
		}
		for (String host : this.hosts) {
			if (targetHost.equals(host)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean checkResourceExists(Locale locale) throws Exception {
		return true;
	}

}
