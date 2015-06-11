/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.web.servlet.mvc.method.annotation;

import java.lang.reflect.Type;

import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterizedTypeAware;
import org.springframework.core.ParameterizedTypeValue;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;

/**
 * Customize the response by retaining the parametrized type information
 * by wrapping the body and the type in a {@link ParameterizedTypeValue}.
 *
 * These additional type information could then be used by {@link HttpMessageConverter}
 * that implements {@link ParameterizedTypeAware} to serialize the body.
 *
 * @since 4.2
 * @author Sebastien Deleuze
 */
public class ParameterizedTypeResponseBodyAdvice implements ResponseBodyAdvice<Object> {

	@Override
	public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
		return ParameterizedTypeAware.class.isAssignableFrom(converterType);
	}

	@Override
	public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType contentType,
			Class<? extends HttpMessageConverter<?>> selectedConverterType,
			ServerHttpRequest request, ServerHttpResponse response) {
		ParameterizedTypeValue container = getOrCreateContainer(body);
		Type type;
		if (HttpEntity.class.isAssignableFrom(returnType.getParameterType())) {
			returnType.increaseNestingLevel();
			type = returnType.getNestedGenericParameterType();
		}
		else {
			type = returnType.getGenericParameterType();
		}
		container.setType(type);
		return container;
	}

	/**
	 * Wrap the body in a {@link ParameterizedTypeValue} value container (for providing
	 * additional information about type) or simply cast it if already wrapped.
	 */
	protected ParameterizedTypeValue getOrCreateContainer(Object body) {
		return (body instanceof ParameterizedTypeValue ? (ParameterizedTypeValue) body : new ParameterizedTypeValue(body));
	}

}
