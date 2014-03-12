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

package org.springframework.web.servlet.view.tiles3;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.ServletContext;

import org.apache.tiles.request.ApplicationResource;
import org.apache.tiles.request.locale.URLApplicationResource;
import org.apache.tiles.request.servlet.ServletApplicationContext;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.ObjectUtils;
import org.springframework.web.context.support.ServletContextResourcePatternResolver;

/**
 * Spring-specific subclass of the Tiles ServletApplicationContext.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @since 3.2
 */
public class SpringWildcardServletTilesApplicationContext extends ServletApplicationContext {

	private final ResourcePatternResolver resolver;

	protected final Pattern localePattern;

	public SpringWildcardServletTilesApplicationContext(ServletContext servletContext) {
		super(servletContext);
		this.resolver = new ServletContextResourcePatternResolver(servletContext);
		this.localePattern = Pattern.compile("(^.*?)_([^_]*)_?([^_]*)?_?([^_]*)?\\..*$");
	}


	@Override
	public ApplicationResource getResource(String localePath) {
		ApplicationResource retValue = null;
		Collection<ApplicationResource> urlSet = getResources(localePath);
		if (urlSet != null && !urlSet.isEmpty()) {
			retValue = urlSet.iterator().next();
		}
		return retValue;
	}

	@Override
	public ApplicationResource getResource(ApplicationResource base, Locale locale) {
		ApplicationResource retValue = null;
		Collection<ApplicationResource> urlSet = getResources(base.getLocalePath(locale));
		if (urlSet != null && !urlSet.isEmpty()) {
			retValue = urlSet.iterator().next();
		}
		return retValue;
	}

	@Override
	public Collection<ApplicationResource> getResources(String path) {
		Resource[] resources;
		try {
			resources = this.resolver.getResources(path);
		}
		catch (IOException ex) {
			return Collections.<ApplicationResource> emptyList();
		}
		Collection<ApplicationResource> resourceList = new ArrayList<ApplicationResource>();
		if (!ObjectUtils.isEmpty(resources)) {
			for (Resource resource : resources) {
				try {
					URL url = resource.getURL();
					Locale locale = this.getLocaleFromPath(url.toExternalForm());
					resourceList.add(new URLApplicationResource(url.toExternalForm(), locale,  url));
				}
				catch (IOException ex) {
					// shouldn't happen with the kind of resources we're using
					throw new IllegalArgumentException("No URL for " + resource.toString(), ex);
				}
			}
		}
		return resourceList;
	}

	/**
	 * Parse <code>localPath</code> in order to extract a valid {@link Locale},
	 * return {@link Locale#ROOT} if not valid {@link Locale} is found.
	 * @since 4.0.3
	 */
	protected Locale getLocaleFromPath(String localPath) {
		Locale locale = Locale.ROOT;
		List<Locale> localesToValidate = new ArrayList<Locale>();
		Matcher matcher = this.localePattern.matcher(localPath);
		if(matcher.matches()) {
			if (matcher.group(3).equals("") && matcher.group(4).equals("")) {
				localesToValidate.add(new Locale(matcher.group(2)));
			} else if (matcher.group(4).equals("")) {
				localesToValidate.add(new Locale(matcher.group(2), matcher.group(3)));
				localesToValidate.add(new Locale(matcher.group(3)));
			} else {
				localesToValidate.add(new Locale(matcher.group(2), matcher.group(3), matcher.group(4)));
				localesToValidate.add(new Locale(matcher.group(3), matcher.group(4)));
				localesToValidate.add(new Locale(matcher.group(4)));
			}
			for (Locale availableLocale : Locale.getAvailableLocales()) {
				for(Locale localeToValidate : localesToValidate) {
					if (availableLocale.equals(localeToValidate)) {
						locale = localeToValidate;
						break;
					}
				}
			}

		}
		return locale;
	}

}

