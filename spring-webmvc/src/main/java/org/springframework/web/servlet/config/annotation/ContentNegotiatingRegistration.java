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

import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.ContentNegotiatingViewResolver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Encapsulates information required to create a content negotiating view resolver.
 *
 * @author Sebastien Deleuze
 * @since 4.1
 */
public class ContentNegotiatingRegistration extends ViewResolutionRegistration<ContentNegotiatingViewResolver> {

	private List<View> defaultViews;

	public ContentNegotiatingRegistration(ViewResolutionRegistry registry) {
		super(registry, new ContentNegotiatingViewResolver());
	}

	public ContentNegotiatingRegistration useNotAcceptable(boolean useNotAcceptable) {
		this.viewResolver.setUseNotAcceptableStatusCode(useNotAcceptable);
		return this;
	}

	public ContentNegotiatingRegistration defaultViews(View... defaultViews) {
		if(this.defaultViews == null) {
			this.defaultViews = new ArrayList<View>();
		}
		this.defaultViews.addAll(Arrays.asList(defaultViews));
		return this;
	}

	@Override
	protected ContentNegotiatingViewResolver getViewResolver() {
		this.viewResolver.setDefaultViews(this.defaultViews);
		return super.getViewResolver();
	}
}
