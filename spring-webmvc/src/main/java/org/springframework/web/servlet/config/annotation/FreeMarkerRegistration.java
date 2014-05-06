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

import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;
import org.springframework.web.servlet.view.freemarker.FreeMarkerViewResolver;

import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates information required to create a FreeMarker view resolver and configurer.
 * Default prefix is set to /WEB-INF and default suffix to .ftl.
 *
 * @author Sebastien Deleuze
 * @since 4.1
 */
public class FreeMarkerRegistration extends ViewResolutionRegistration<FreeMarkerViewResolver> {

	private final FreeMarkerConfigurer configurer;
	private List<String> templateLoaderPaths;

	public FreeMarkerRegistration(ViewResolutionRegistry registry, FreeMarkerConfigurer configurer) {
		super(registry, new FreeMarkerViewResolver());
		this.configurer = configurer;
		this.prefix("/WEB-INF");
		this.suffix(".ftl");
	}

	public FreeMarkerRegistration templateLoaderPath(String templateLoaderPath) {
		if(this.templateLoaderPaths == null) {
			this.templateLoaderPaths = new ArrayList<String>();
		}
		this.templateLoaderPaths.add(templateLoaderPath);
		return this;
	}

	public FreeMarkerRegistration prefix(String prefix) {
		this.viewResolver.setPrefix(prefix);
		return this;
	}

	public FreeMarkerRegistration suffix(String suffix) {
		this.viewResolver.setSuffix(suffix);
		return this;
	}

	public FreeMarkerRegistration cache(boolean cache) {
		this.viewResolver.setCache(cache);
		return this;
	}

	@Override
	protected Object getConfigurer() {
		if(this.templateLoaderPaths != null && !this.templateLoaderPaths.isEmpty()) {
			this.configurer.setTemplateLoaderPaths(this.templateLoaderPaths.toArray(new String[0]));
		}
		return this.configurer;
	}
}
