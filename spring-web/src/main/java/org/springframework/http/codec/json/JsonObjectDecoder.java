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

package org.springframework.http.codec.json;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.AbstractDecoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.util.MimeType;

/**
 * Decode an arbitrary split byte stream representing JSON objects to a byte
 * stream where each chunk is a well-formed JSON object.
 *
 * <p>This class does not do any real parsing or validation. A sequence of bytes
 * is considered a JSON object/array if it contains a matching number of opening
 * and closing braces/brackets.
 *
 * <p>Based on <a href="https://github.com/netty/netty/blob/master/codec/src/main/java/io/netty/handler/codec/json/JsonObjectDecoder.java">Netty JsonObjectDecoder</a>
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
class JsonObjectDecoder extends AbstractDecoder<DataBuffer> {

	private static final int ST_CORRUPTED = -1;

	private static final int ST_INIT = 0;

	private static final int ST_DECODING_NORMAL = 1;

	private static final int ST_DECODING_ARRAY_STREAM = 2;

	private final int maxLength;

	private final boolean streamArrayElements;


	public JsonObjectDecoder() {
		// 1 MB
		this(1024 * 1024);
	}

	public JsonObjectDecoder(int maxLength) {
		this(maxLength, true);
	}

	public JsonObjectDecoder(boolean streamArrayElements) {
		this(1024 * 1024, streamArrayElements);
	}


	/**
	 * @param maxLength maximum number of bytes a JSON object/array may
	 * use (including braces and all). Objects exceeding this length are dropped
	 * and an {@link IllegalStateException} is thrown.
	 * @param streamArrayElements if set to true and the "top level" JSON object
	 * is an array, each of its entries is passed through the pipeline individually
	 * and immediately after it was fully received, allowing for arrays with
	 */
	public JsonObjectDecoder(int maxLength, boolean streamArrayElements) {
		super(new MimeType("application", "json", StandardCharsets.UTF_8),
				new MimeType("application", "*+json", StandardCharsets.UTF_8));
		if (maxLength < 1) {
			throw new IllegalArgumentException("maxLength must be a positive int");
		}
		this.maxLength = maxLength;
		this.streamArrayElements = streamArrayElements;
	}

	@Override
	public Flux<DataBuffer> decode(Publisher<DataBuffer> inputStream, ResolvableType elementType,
			MimeType mimeType, Object... hints) {

		return Flux.from(inputStream).flatMap(new JsonObjectMapper());
	}

	private class JsonObjectMapper implements Function<DataBuffer, Publisher<? extends DataBuffer>> {

		int openBraces;
		int state;
		boolean insideString;
		DataBuffer input;
		int objectIndex = 0;
		int objectLength = 0;
		int skip = 0;
		int skipLast = 0;
		byte previous;

		@Override
		public Publisher<? extends DataBuffer> apply(DataBuffer buffer) {
			int bufferIndex = 0;
			List<DataBuffer> chunks = new ArrayList<>();
			if (this.input == null) {
				this.input = buffer;
			}
			else {
				this.input.write(buffer);
			}
			if (this.state == ST_CORRUPTED) {
				return Flux.error(new IllegalStateException("Corrupted stream"));
			}
			int length = this.objectIndex + this.objectLength + buffer.readableByteCount();
			if (length > maxLength) {
				// buffer size exceeded maxObjectLength; discarding the complete buffer.
				reset();
				return Flux.error(new IllegalStateException("object length exceeds " +
						maxLength + ": " + objectLength + " bytes received"));
			}
			DataBuffer slice = buffer.slice(0, buffer.readableByteCount());
			while (slice.readableByteCount() > 0) {
				bufferIndex++;
				this.objectLength++;
				byte c = slice.read();
				if (this.state == ST_DECODING_NORMAL) {
					decodeByte(c, previous);

					// All opening braces/brackets have been closed. That's enough to conclude
					// that the JSON object/array is complete.
					if (this.openBraces == 0) {
						chunks.add(extractObject());

						// Reset the object state to get ready for the next JSON object/text
						// coming along the byte stream.
						reset();
					}
				}
				else if (this.state == ST_DECODING_ARRAY_STREAM) {
					decodeByte(c, previous);

					if (!this.insideString && (this.openBraces == 1 && c == ',' ||
							this.openBraces == 0 && c == ']')) {

						this.skipLast++;
						chunks.add(DataBufferUtils.retain(extractObject()));

						if (c == ']') {
							reset();
						}
					}
					// JSON object/array detected. Accumulate bytes until all braces/brackets are closed.
				}
				else if (c == '{' || c == '[') {
					initDecoding(c, streamArrayElements);

					if (this.state == ST_DECODING_ARRAY_STREAM) {
						// Discard the array bracket
						this.skip++;
					}
				}
				else if (Character.isWhitespace(c)) {
					this.skip++;
				}
				else {
					this.state = ST_CORRUPTED;
					return Flux.error(new IllegalStateException(
							"invalid JSON received at byte position " + (this.objectIndex + bufferIndex)));
				}
				previous = c;
			}
			return Flux.fromIterable(chunks);
		}

		private DataBuffer extractObject() {
			// Start index without leading space
			int startIndex = this.input.indexOf(i -> !Character.isWhitespace(i), this.objectIndex + this.skip);
			this.skip += startIndex - (this.objectIndex + this.skip);
			int length = this.objectLength - this.skip - skipLast;
			// New length without trailing spaces
			length = this.input.lastIndexOf(i -> !Character.isWhitespace(i), startIndex + length - 1) - startIndex + 1;
			DataBuffer json = this.input.slice(startIndex, length);
			this.objectIndex += this.objectLength;
			this.objectLength = 0;
			this.skip = 0;
			this.skipLast = 0;
			return json;
		}

		private void decodeByte(byte c, byte previous) {
			if ((c == '{' || c == '[') && !this.insideString) {
				this.openBraces++;
			}
			else if ((c == '}' || c == ']') && !this.insideString) {
				this.openBraces--;
			}
			else if (c == '"') {
				// start of a new JSON string. It's necessary to detect strings as they may
				// also contain braces/brackets and that could lead to incorrect results.
				if (!this.insideString) {
					this.insideString = true;
					// If the double quote wasn't escaped then this is the end of a string.
				}
				else if (previous != '\\') {
					this.insideString = false;
				}
			}
		}

		private void initDecoding(byte openingBrace, boolean streamArrayElements) {
			this.openBraces = 1;
			if (openingBrace == '[' && streamArrayElements) {
				this.state = ST_DECODING_ARRAY_STREAM;
			}
			else {
				this.state = ST_DECODING_NORMAL;
			}
		}

		private void reset() {
			this.insideString = false;
			this.state = ST_INIT;
			this.openBraces = 0;
		}
	}

}
