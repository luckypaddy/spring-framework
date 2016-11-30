/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.reactive.result.view.freemarker;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapperBuilder;
import freemarker.template.ObjectWrapper;
import freemarker.template.SimpleHash;
import freemarker.template.Template;
import freemarker.template.Version;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.reactive.result.view.AbstractUrlBasedView;
import org.springframework.web.server.ServerWebExchange;

/**
 * A {@code View} implementation that uses the FreeMarker template engine.
 *
 * <p>Note: Spring's FreeMarker support requires FreeMarker 2.3 or higher.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @since 5.0
 */
public class FreeMarkerView extends AbstractUrlBasedView {

	private final Configuration configuration;

	private String encoding;


	public FreeMarkerView(String url, Configuration configuration) {
		super(url);
		Assert.notNull(configuration, "Freemarker Configuration must not be null");
		this.configuration = configuration;
	}

	/**
	 * Return the FreeMarker configuration used by this view.
	 */
	protected Configuration getConfiguration() {
		return this.configuration;
	}

	/**
	 * Set the encoding of the FreeMarker template file.
	 * <p>By default {@link FreeMarkerConfigurer} sets the default encoding in
	 * the FreeMarker configuration to "UTF-8". It's recommended to specify the
	 * encoding in the FreeMarker Configuration rather than per template if all
	 * your templates share a common encoding.
	 */
	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	/**
	 * Return the encoding for the FreeMarker template.
	 */
	protected String getEncoding() {
		return this.encoding;
	}

	/**
	 * Check that the FreeMarker template used for this view exists and is valid.
	 * <p>Can be overridden to customize the behavior, for example in case of
	 * multiple templates to be rendered into a single view.
	 */
	@Override
	public boolean checkResourceExists(Locale locale) throws Exception {
		try {
			// Check that we can get the template, even if we might subsequently get it again.
			getTemplate(locale);
			return true;
		}
		catch (FileNotFoundException ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("No FreeMarker view found for URL: " + getUrl());
			}
			return false;
		}
		catch (IOException ex) {
			throw new IllegalStateException(
					"Could not load FreeMarker template for URL [" + getUrl() + "]", ex);
		}
	}

	@Override
	protected Mono<Void> renderInternal(Map<String, Object> renderAttributes, MediaType contentType,
			ServerWebExchange exchange) {
		// Expose all standard FreeMarker hash models.
		SimpleHash freeMarkerModel = getTemplateModel(renderAttributes, exchange);
		if (logger.isDebugEnabled()) {
			logger.debug("Rendering FreeMarker template [" + getUrl() + "].");
		}
		Locale locale = Locale.getDefault(); // TODO
		DataBuffer dataBuffer = exchange.getResponse().bufferFactory().allocateBuffer();
		try {
			Charset charset = getCharset(contentType).orElse(getDefaultCharset());
			Writer writer = new OutputStreamWriter(dataBuffer.asOutputStream(), charset);
			getTemplate(locale).process(freeMarkerModel, writer);
		}
		catch (IOException ex) {
			String message = "Could not load FreeMarker template for URL [" + getUrl() + "]";
			return Mono.error(new IllegalStateException(message, ex));
		}
		catch (Throwable ex) {
			return Mono.error(ex);
		}
		return exchange.getResponse().writeWith(Flux.just(dataBuffer));
	}

	private static Optional<Charset> getCharset(MediaType mediaType) {
		return mediaType != null ? Optional.ofNullable(mediaType.getCharset()) : Optional.empty();
	}

	/**
	 * Build a FreeMarker template model for the given model Map.
	 * <p>The default implementation builds a {@link SimpleHash}.
	 * @param model the model to use for rendering
	 * @param exchange current exchange
	 * @return the FreeMarker template model, as a {@link SimpleHash} or subclass thereof
	 */
	protected SimpleHash getTemplateModel(Map<String, Object> model, ServerWebExchange exchange) {
		SimpleHash fmModel = new SimpleHash(getObjectWrapper());
		fmModel.putAll(model);
		return fmModel;
	}

	/**
	 * Return the configured FreeMarker {@link ObjectWrapper}, or the
	 * {@link ObjectWrapper#DEFAULT_WRAPPER default wrapper} if none specified.
	 * @see freemarker.template.Configuration#getObjectWrapper()
	 */
	protected ObjectWrapper getObjectWrapper() {
		ObjectWrapper ow = getConfiguration().getObjectWrapper();
		Version version = Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS;
		return (ow != null ? ow : new DefaultObjectWrapperBuilder(version).build());
	}

	/**
	 * Retrieve the FreeMarker template for the given locale,
	 * to be rendering by this view.
	 * <p>By default, the template specified by the "url" bean property
	 * will be retrieved.
	 * @param locale the current locale
	 * @return the FreeMarker template to render
	 */
	protected Template getTemplate(Locale locale) throws IOException {
		return (getEncoding() != null ?
				getConfiguration().getTemplate(getUrl(), locale, getEncoding()) :
				getConfiguration().getTemplate(getUrl(), locale));
	}

}
