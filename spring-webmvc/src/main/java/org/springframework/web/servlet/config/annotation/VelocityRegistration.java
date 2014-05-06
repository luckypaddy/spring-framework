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

import org.springframework.web.servlet.view.velocity.VelocityConfigurer;
import org.springframework.web.servlet.view.velocity.VelocityViewResolver;

/**
 * Encapsulates information required to create a Velocity view resolver and configurer.
 * Default prefix is set to /WEB-INF and default suffix to .vm.
 *
 * @author Sebastien Deleuze
 * @since 4.1
 */
public class VelocityRegistration extends ViewResolutionRegistration<VelocityViewResolver> {

	private final VelocityConfigurer configurer;

	public VelocityRegistration(ViewResolutionRegistry registry, VelocityConfigurer configurer) {
		super(registry, new VelocityViewResolver());
		this.configurer = configurer;
		this.prefix("/WEB-INF");
		this.suffix(".vm");
	}

	public VelocityRegistration resourceLoaderPath(String resourceLoaderPath) {
		this.configurer.setResourceLoaderPath(resourceLoaderPath);
		return this;
	}

	public VelocityRegistration prefix(String prefix) {
		this.viewResolver.setPrefix(prefix);
		return this;
	}

	public VelocityRegistration suffix(String suffix) {
		this.viewResolver.setSuffix(suffix);
		return this;
	}

	public VelocityRegistration cache(boolean cache) {
		this.viewResolver.setCache(cache);
		return this;
	}

	@Override
	protected Object getConfigurer() {
		return this.configurer;
	}
}
