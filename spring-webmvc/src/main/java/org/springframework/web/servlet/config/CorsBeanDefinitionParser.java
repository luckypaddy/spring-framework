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

package org.springframework.web.servlet.config;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.parsing.CompositeComponentDefinition;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.xml.DomUtils;
import org.springframework.web.servlet.cors.CorsConfigSpecification;
import org.springframework.web.servlet.cors.CorsHandler;
import org.w3c.dom.Element;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link org.springframework.beans.factory.xml.BeanDefinitionParser} that parses a
 * {@code interceptors} element to register {@link org.springframework.web.servlet.handler.MappedInterceptor}
 * definitions for simple request and {@link org.springframework.web.servlet.handler.SimpleUrlHandlerMapping}
 * definitions for preflight requests.
 *
 * @author Sebastien Deleuze
 * @since 4.1
 */
public class CorsBeanDefinitionParser implements BeanDefinitionParser {

	@Override
	public BeanDefinition parse(Element element, ParserContext parserContext) {
		CompositeComponentDefinition compDefinition = new CompositeComponentDefinition(element.getTagName(), parserContext.extractSource(element));
		parserContext.pushContainingComponent(compDefinition);

		RootBeanDefinition handlerDef = new RootBeanDefinition(CorsHandler.class);
		handlerDef.setSource(parserContext.extractSource(element));
		handlerDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		handlerDef.getPropertyValues().add("order", "-1");
		Map<String, CorsConfigSpecification> urlConfigMap = new HashMap<String, CorsConfigSpecification>();

		List<Element> corsElements = DomUtils.getChildElementsByTagName(element, new String[] { "handler" });

		for (Element corsElement : corsElements) {
			registerHandler(corsElement, urlConfigMap);
		}

		if(corsElements.isEmpty()) {
			CorsConfigSpecification config = new CorsConfigSpecification();
			config.addAllowedOrigin("*");
			urlConfigMap.put("/**", config);
		}

		handlerDef.getPropertyValues().add("urlConfigMap", urlConfigMap);
		String beanName = parserContext.getReaderContext().registerWithGeneratedName(handlerDef);
		parserContext.registerComponent(new BeanComponentDefinition(handlerDef, beanName));
		parserContext.popAndRegisterContainingComponent();
		return null;
	}

	private ManagedList<String> getMapping(Element interceptor, String elementName) {
		List<Element> paths = DomUtils.getChildElementsByTagName(interceptor, elementName);
		ManagedList<String> patterns = new ManagedList<String>(paths.size());
		for (Element path : paths) {
			patterns.add(path.getAttribute("path"));
		}
		if(patterns.isEmpty()) {
			patterns = null;
		}
		return patterns;
	}

	private void registerHandler(Element corsElement, Map<String, CorsConfigSpecification> urlConfigMap) {
		CorsConfigSpecification config = new CorsConfigSpecification();
		String allowedOrigins = corsElement.getAttribute("allowed-origins");
		if(allowedOrigins.isEmpty()) {
			if(allowedOrigins.isEmpty()) {
				config.addAllowedOrigin("*");
			}
		}
		else {
			String[] splittedAllowedOrigins = allowedOrigins.split(",");
			for(int i = 0 ; i < splittedAllowedOrigins.length ; i++) {
				splittedAllowedOrigins[i] = splittedAllowedOrigins[i].trim();
			}
			config.setAllowedOrigins(Arrays.asList(splittedAllowedOrigins));
		}
		String exposedHeaders = corsElement.getAttribute("exposed-headers");
		if(!exposedHeaders.isEmpty()) {
			String[] splittedExposedHeaders = exposedHeaders.split(",");
			for(int i = 0 ; i < splittedExposedHeaders.length ; i++) {
				splittedExposedHeaders[i] = splittedExposedHeaders[i].trim();
			}
			config.setExposedHeaders(Arrays.asList(splittedExposedHeaders));
		}
		String allowCredentials = corsElement.getAttribute("allow-credentials");
		if(!allowCredentials.isEmpty()) {
			config.setAllowCredentials(Boolean.valueOf(allowCredentials));
		}
		String allowedHeaders = corsElement.getAttribute("allowed-headers");
		if(!allowedHeaders.isEmpty()) {
			String[] splittedAllowedHeaders = allowedHeaders.split(",");
			for(int i = 0 ; i < splittedAllowedHeaders.length ; i++) {
				splittedAllowedHeaders[i] = splittedAllowedHeaders[i].trim();
			}
			config.setAllowedHeaders(Arrays.asList(splittedAllowedHeaders));
		}
		String allowedMethods = corsElement.getAttribute("allowed-methods");
		if(!allowedMethods.isEmpty()) {
			String[] splittedAllowedMethods = allowedMethods.split(",");
			for(int i = 0 ; i < splittedAllowedMethods.length ; i++) {
				splittedAllowedMethods[i] = splittedAllowedMethods[i].trim();
			}
			config.setAllowedMethods(Arrays.asList(splittedAllowedMethods));
		}
		String maxAge = corsElement.getAttribute("max-age");
		if(!maxAge.isEmpty()) {
			config.setMaxAge(Long.valueOf(maxAge));
		}

		ManagedList<String> includePatterns = getMapping(corsElement, "mapping");
		if(includePatterns != null) {
			for(String includePattern : includePatterns) {
				urlConfigMap.put(includePattern, config);
			}
		}
		else {
			urlConfigMap.put("/**", config);
		}
	}
}
