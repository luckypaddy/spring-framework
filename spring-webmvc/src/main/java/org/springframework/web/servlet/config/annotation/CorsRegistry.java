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
import org.springframework.web.servlet.cors.CorsHandler;

import java.util.*;

/**
 * Helps with configuring a list of CORS interceptors and handler mappings.
 *
 * @author Sebastien Deleuze
 * @since 4.1
 * @see <a href="http://www.w3.org/TR/cors/">CORS specification</a>
 */
public class CorsRegistry {

	private final List<CorsRegistration> registrations = new ArrayList<CorsRegistration>();
	private int order = -1;

	/**
	 * Add a new CORS handler mapping. By default, if not paths are specified, all the
	 * requests are mapped.
	 */
	public CorsRegistration addCorsHandlerMapping(String... paths) {
		CorsRegistration registration = new CorsRegistration();
		registration.paths.addAll(Arrays.asList(paths));
		registrations.add(registration);
		return registration;
	}

	/**
	 * Specify the order to use for CORS preflight request handler mappings relative to other
	 * {@link org.springframework.web.servlet.HandlerMapping}s configured in the Spring MVC
	 * application context. The default value is -1, which is 1 lower than the value used
	 * for annotated controllers.
	 */
	public void setOrder(int order) {
		this.order = order;
	}

	/**
	 * Returns the handler mapping for CORS preflight request
	 */
	protected CorsHandler getCorsHandler() {
		CorsHandler corsHandler = new CorsHandler();

		for (CorsRegistration registration : this.registrations) {
			CorsConfigSpecification config = registration.getConfig();
			if(config.getAllowedOrigins().isEmpty()) {
				config.addAllowedOrigin("*");
			}
			List<String> paths = registration.getPaths();
			if(paths.isEmpty()) {
				paths.add("/**");
			}
			for(String path : paths) {
				corsHandler.addCorsMapping(path, config);
			}
		}

		corsHandler.setOrder(this.order);
		return corsHandler;
	}

}
