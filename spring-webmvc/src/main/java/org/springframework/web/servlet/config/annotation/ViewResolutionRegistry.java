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
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.ViewResolutionRegistration.*;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;
import org.springframework.web.servlet.view.tiles2.TilesConfigurer;
import org.springframework.web.servlet.view.velocity.VelocityConfigurer;

import java.util.ArrayList;
import java.util.List;

/**
 * Helps with configuring a list of view resolvers.
 *
 * @author Sebastien Deleuze
 * @since 4.1
 */
public class ViewResolutionRegistry {

	private final List<ViewResolutionRegistration<?>> registrations = new ArrayList<ViewResolutionRegistration<?>>();

	public ViewResolutionRegistration<ViewResolver> addViewResolver(ViewResolver viewResolver) {
		ViewResolutionRegistration<ViewResolver> registration = new ViewResolutionRegistration<ViewResolver>(this, viewResolver);
		registrations.add(registration);
		return registration;
	}

	public JspRegistration jsp() {
		JspRegistration registration = new JspRegistration(this);
		registrations.add(registration);
		return registration;
	}

	public JspRegistration jsp(String prefix, String suffix) {
		JspRegistration registration = new JspRegistration(this, prefix, suffix);
		registrations.add(registration);
		return registration;
	}

	public BeanNameRegistration beanName() {
		BeanNameRegistration registration = new BeanNameRegistration(this);
		registrations.add(registration);
		return registration;
	}

	public TilesRegistration tiles(TilesConfigurer configurer) {
		TilesRegistration registration = new TilesRegistration(this, configurer);
		registrations.add(registration);
		return registration;
	}

	public VelocityRegistration velocity(VelocityConfigurer configurer) {
		VelocityRegistration registration = new VelocityRegistration(this, configurer);
		registrations.add(registration);
		return registration;
	}

	public FreeMarkerRegistration freemarker(FreeMarkerConfigurer configurer) {
		FreeMarkerRegistration registration = new FreeMarkerRegistration(this, configurer);
		registrations.add(registration);
		return registration;
	}

	public ContentNegotiatingRegistration contentNegotiating(View... defaultViews) {
		ContentNegotiatingRegistration registration = new ContentNegotiatingRegistration(this);
		registration.defaultViews(defaultViews);
		registrations.add(registration);
		return registration;
	}

	protected List<ViewResolver> getViewResolvers() {
		List<ViewResolver> viewResolvers = new ArrayList<ViewResolver>();

		for(ViewResolutionRegistration<?> registration : this.registrations) {
			viewResolvers.add(registration.getViewResolver());
		}
		return viewResolvers;
	}

	protected List<Object> getConfigurers() {
		List<Object> configurers = new ArrayList<Object>();

		for(ViewResolutionRegistration<?> registration : this.registrations) {
			Object configurer = registration.getConfigurer();
			if(configurer != null)
				configurers.add(configurer);
		}
		return configurers;
	}
}
