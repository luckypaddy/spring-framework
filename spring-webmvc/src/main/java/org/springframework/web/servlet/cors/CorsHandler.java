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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.AsyncHandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.AbstractHandlerMapping;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

/**
 * This class handles CORS requests thanks to 3 main capabilities :
 *  - It defines a collection of mapped URL patterns
 *  - Simple or actual requests are handled by this CorsHandler acting as a
 *    {@link org.springframework.web.servlet.HandlerInterceptor} automatically detected and
 *    used by all {@link AbstractHandlerMapping} beans
 *  - Preflight requests are handled by this CorsHandler acting as a {@link HttpRequestHandler}
 *
 * @author Sebastien Deleuze
 * @since 4.1
 * @see <a href="http://www.w3.org/TR/cors/">CORS specification</a>
 * @see org.springframework.web.servlet.config.annotation.WebMvcConfigurer#configureCors(org.springframework.web.servlet.config.annotation.CorsRegistry)
 */
public class CorsHandler extends AbstractHandlerMapping implements HttpRequestHandler, AsyncHandlerInterceptor {

	// Request headers
	protected static final String HEADER_ORIGIN = "Origin";
	protected static final String HEADER_ACCESS_CONTROL_REQUEST_HEADERS = "Access-Control-Request-Headers";
	protected static final String HEADER_ACCESS_CONTROL_REQUEST_METHOD = "Access-Control-Request-Method";

	// Response headers
	protected static final String HEADER_ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
	protected static final String HEADER_ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";
	protected static final String HEADER_ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";
	protected static final String HEADER_ACCESS_CONTROL_MAX_AGE = "Access-Control-Max-Age";
	protected static final String HEADER_ACCESS_CONTROL_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";
	protected static final String HEADER_ACCESS_CONTROL_EXPOSE_HEADERS = "Access-Control-Expose-Headers";

	protected final Log logger = LogFactory.getLog(getClass());

	protected Map<String, CorsConfigSpecification> urlConfigMap;

	public CorsHandler() {
		this.urlConfigMap = new HashMap<String, CorsConfigSpecification>();
	}

	public Map<String, CorsConfigSpecification> getUrlConfigMap() {
		return urlConfigMap;
	}

	public void setUrlConfigMap(Map<String, CorsConfigSpecification> urlConfigMap) {
		this.urlConfigMap = urlConfigMap;
	}

	public void addCorsMapping(String path, CorsConfigSpecification config) {
		this.urlConfigMap.put(path, config);
	}

	@Override
	protected void initCorsHandler() {
		// No op in order to avoid CORS interceptor
	}


	// URL mapping
	@Override
	protected Object getHandlerInternal(HttpServletRequest request) throws Exception {
		if(request.getMethod().equals(RequestMethod.OPTIONS.name()) && request.getHeader(HEADER_ORIGIN) != null) {
			return this;
		}
		return null;
	}


	// Interceptor
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		try {
			CorsConfigSpecification config = this.lookupConfig(request, this.urlConfigMap);
			this.checkOrigin(config, request, response);
			if(!config.getExposedHeaders().isEmpty()) {
				response.addHeader(HEADER_ACCESS_CONTROL_EXPOSE_HEADERS, StringUtils.collectionToDelimitedString(config.getExposedHeaders(), ", "));
			}
		} catch (CorsException ce) {
			if(ce.isLogged()) {
				logger.debug(ce.getMessage());
			}
			if(!ce.isSkipped()) {
				response.sendError(HttpServletResponse.SC_FORBIDDEN, ce.getMessage());
			}
			return false;
		}
		return true;
	}

	// Request handler
	@Override
	public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		try {

			CorsConfigSpecification config = this.lookupConfig(request, this.urlConfigMap);
			if(!request.getMethod().equals(RequestMethod.OPTIONS.name())) {
				throw new CorsException("CorsPreflightRequestHandler should handle only OPTIONS requests");
			}
			this.checkOrigin(config, request, response);
			List<String> matchingHeaders = checkHeaders(config, request, response);
			if (config.getMaxAge() != null) {
				response.addHeader(HEADER_ACCESS_CONTROL_MAX_AGE, config.getMaxAge().toString());
			}
			List<String> allowMethods = checkMethod(config, request, response);
			response.addHeader(HEADER_ACCESS_CONTROL_ALLOW_METHODS, StringUtils.collectionToDelimitedString(allowMethods, ", "));
			response.addHeader(HEADER_ACCESS_CONTROL_ALLOW_HEADERS, StringUtils.collectionToDelimitedString(matchingHeaders, ", "));
		} catch (CorsException ce) {
			if(ce.isLogged()) {
				logger.debug(ce.getMessage());
			}
			response.sendError(HttpServletResponse.SC_FORBIDDEN, ce.getMessage());
		}
	}

	protected CorsConfigSpecification lookupConfig(HttpServletRequest request, Map<String, ?> urlConfigMap) {
		// Direct match?
		String urlPath = getUrlPathHelper().getLookupPathForRequest(request);
		CorsConfigSpecification config = (CorsConfigSpecification)urlConfigMap.get(urlPath);
		if (config != null) {
			return config;
		}
		// Pattern match?
		List<String> matchingPatterns = new ArrayList<String>();
		for (String registeredPattern : urlConfigMap.keySet()) {
			if (getPathMatcher().match(registeredPattern, urlPath)) {
				matchingPatterns.add(registeredPattern);
			}
		}
		String bestPatternMatch = null;
		Comparator<String> patternComparator = getPathMatcher().getPatternComparator(urlPath);
		if (!matchingPatterns.isEmpty()) {
			Collections.sort(matchingPatterns, patternComparator);
			if (logger.isDebugEnabled()) {
				logger.debug("Matching patterns for request [" + urlPath + "] are " + matchingPatterns);
			}
			bestPatternMatch = matchingPatterns.get(0);
		}
		if (bestPatternMatch != null) {
			config = (CorsConfigSpecification)urlConfigMap.get(bestPatternMatch);

			// There might be multiple 'best patterns', let's make sure we have the correct URI template variables
			// for all of them
			Map<String, String> uriTemplateVariables = new LinkedHashMap<String, String>();
			for (String matchingPattern : matchingPatterns) {
				if (patternComparator.compare(bestPatternMatch, matchingPattern) == 0) {
					Map<String, String> vars = getPathMatcher().extractUriTemplateVariables(matchingPattern, urlPath);
					Map<String, String> decodedVars = getUrlPathHelper().decodePathVariables(request, vars);
					uriTemplateVariables.putAll(decodedVars);
				}
			}
			if (logger.isDebugEnabled()) {
				logger.debug("URI Template variables for request [" + urlPath + "] are " + uriTemplateVariables);
			}
			return config;
		}
		throw new CorsException("No matching config found!");
	}

	// CORS implementation
	protected void checkOrigin(CorsConfigSpecification config,  HttpServletRequest request, HttpServletResponse response) throws IOException {
		if (response.getHeader(HEADER_ACCESS_CONTROL_ALLOW_ORIGIN) != null) {
			throw new CorsException("Skip adding CORS headers, response already contains \"Access-Control-Allow-Origin\"", true, true);
		}
		String origin = request.getHeader(HEADER_ORIGIN);
		if(origin == null) {
			throw new CorsException("Not a CORS request: no origin header", true, false);
		}
		if(config.getAllowedOrigins().contains("*") && !config.isAllowCredentials()) {
			response.addHeader(HEADER_ACCESS_CONTROL_ALLOW_ORIGIN, "*");
			return;
		}
		boolean isOriginAllowed = false;
		for(String allowedOrigin : config.getAllowedOrigins()) {
			if(origin.equalsIgnoreCase(allowedOrigin) || allowedOrigin.equals("*")) {
				isOriginAllowed = true;
				break;
			}
		}
		if(!isOriginAllowed) {
			throw new CorsException("Invalid CORS request: origin not allowed");
		}
		response.addHeader(HEADER_ACCESS_CONTROL_ALLOW_ORIGIN, origin);
		if(config.isAllowCredentials()) {
			response.addHeader(HEADER_ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
		}
	}

	private List<String> checkMethod(CorsConfigSpecification config, HttpServletRequest request, HttpServletResponse response) throws IOException {
		Enumeration<String> requestAllowMethods = request.getHeaders(HEADER_ACCESS_CONTROL_REQUEST_METHOD);
		List<String> responseAllowMethods = new ArrayList<String>();
		if (!requestAllowMethods.hasMoreElements()) {
			throw new CorsException("Invalid CORS request: mandatory request header Access-Control-Allow-Methods not found!");
		}
		boolean matchAll = config.getAllowedMethods().contains("*");
		boolean matchProvided = false;
		while(requestAllowMethods.hasMoreElements()){
			String requestAllowMethod = requestAllowMethods.nextElement();
			if(matchAll) {
				responseAllowMethods.add(requestAllowMethod);
			}
			else {
				for(String configMethod : config.getAllowedMethods()) {
					if(requestAllowMethod.equalsIgnoreCase(configMethod)) {
						matchProvided = true;
						break;
					}
				}
			}
		}

		if(matchProvided) {
			for(String configMethod : config.getAllowedMethods()) {
				responseAllowMethods.add(configMethod);
			}
		}

		if(responseAllowMethods.isEmpty()) {
			throw new CorsException("Invalid CORS request: method not allowed");
		}

		return responseAllowMethods;
	}

	private List<String> checkHeaders(CorsConfigSpecification config, HttpServletRequest request, HttpServletResponse response) throws IOException {
		List<String> matchingHeaders = new ArrayList<String>();
		Enumeration<String> requestHeaders = request.getHeaders(HEADER_ACCESS_CONTROL_REQUEST_HEADERS);
		if (!requestHeaders.hasMoreElements()) {
			return matchingHeaders;
		}

		String[] splittedRequestHeaders = requestHeaders.nextElement().split(",");
		for(String configHeader : config.getAllowedHeaders()) {
			for(String requestHeader : splittedRequestHeaders) {
				requestHeader = requestHeader.trim();
				if (requestHeader.equalsIgnoreCase(configHeader)) {
					matchingHeaders.add(configHeader);
					break;
				}
			}
		}
		if(matchingHeaders.isEmpty()) {
			throw new CorsException("Invalid CORS request: header(s) not allowed");
		}

		return matchingHeaders;
	}

	/**
	 * This implementation is empty.
	 */
	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {

	}

	/**
	 * This implementation is empty.
	 */
	@Override
	public void afterCompletion(
			HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
			throws Exception {
	}

	/**
	 * This implementation is empty.
	 */
	@Override
	public void afterConcurrentHandlingStarted(
			HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
	}

	private static class CorsException extends RuntimeException {

		private static final long serialVersionUID = -8317778440033097903L;

		private boolean skipped = false;
		private boolean logged = true;

		CorsException(String message) {
			super(message);
		}

		/**
		 * @param skipped Specify if the request processing should be skipped
		 * @param logged Specify if the error be logged
		 */
		CorsException(String message, boolean skipped, boolean logged) {
			super(message);
			this.skipped = skipped;
			this.logged = logged;
		}

		public boolean isSkipped() {
			return skipped;
		}

		public boolean isLogged() {
			return logged;
		}
	}
}
