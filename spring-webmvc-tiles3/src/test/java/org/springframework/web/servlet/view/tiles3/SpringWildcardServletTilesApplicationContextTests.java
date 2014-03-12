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

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.mock.web.test.MockServletContext;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test fixture for {@link SpringWildcardServletTilesApplicationContext}.
 *
 * @author Sebastien Deleuze
 */
public class SpringWildcardServletTilesApplicationContextTests {

	private SpringWildcardServletTilesApplicationContext context;

	@Before
	public void setUp() {
		MockServletContext servletContext = new MockServletContext();
		context = new SpringWildcardServletTilesApplicationContext(servletContext);
	}

	// SPR-11491
	@Test
	public void localePathPattern() {
		Pattern localePattern = (Pattern)new DirectFieldAccessor(this.context).getPropertyValue("localePattern");
		Matcher matcher;
		assertNotNull(localePattern);
		matcher = localePattern.matcher("/tiles.xml");
		assertFalse(matcher.matches());
		matcher = localePattern.matcher("/tiles_fr.xml");
		assertTrue(matcher.matches());
		assertEquals("fr", matcher.group(2));
		matcher = localePattern.matcher("/tiles_fr_FR.xml");
		assertTrue(matcher.matches());
		assertEquals("fr", matcher.group(2));
		assertEquals("FR", matcher.group(3));
		assertEquals("", matcher.group(4));
		matcher = localePattern.matcher("/tiles_fr_FR_variant.xml");
		assertTrue(matcher.matches());
		assertEquals("fr", matcher.group(2));
		assertEquals("FR", matcher.group(3));
		assertEquals("variant", matcher.group(4));
		matcher = localePattern.matcher("/tiles_definition_fr_FR_variant.xml");
		assertTrue(matcher.matches());
		assertEquals("fr", matcher.group(2));
		assertEquals("FR", matcher.group(3));
		assertEquals("variant", matcher.group(4));
	}

	// SPR-11491
	@Test
	public void localeFromPath() {
		assertEquals(Locale.ROOT, this.context.getLocaleFromPath("/tiles.xml"));
		assertEquals(Locale.ROOT, this.context.getLocaleFromPath("/tiles_definition.xml"));
		assertEquals(Locale.FRENCH, this.context.getLocaleFromPath("/tiles_fr.xml"));
		assertEquals(Locale.FRENCH, this.context.getLocaleFromPath("/tiles_definition_fr.xml"));
		assertEquals(Locale.FRANCE, this.context.getLocaleFromPath("/tiles_fr_FR.xml"));
		assertEquals(Locale.FRANCE, this.context.getLocaleFromPath("/tiles_definition_fr_FR.xml"));
	}
}
