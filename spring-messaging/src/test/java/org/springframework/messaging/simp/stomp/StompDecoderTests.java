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

package org.springframework.messaging.simp.stomp;

import org.junit.Test;
import org.springframework.messaging.Message;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import static org.junit.Assert.*;

/**
 * Test fixture for {@link StompDecoder}.
 *
 * @author Sebastien Deleuze
 */
public class StompDecoderTests {

	@Test
	public void singleFragment() throws InterruptedException {
		StompDecoder stompDecoder = new StompDecoder();
		String payload = "SEND\na:alpha\n\nMessage body\0";
		Message<byte[]> message = stompDecoder.decode(this.wrap(payload));
		assertNotNull(message);
		assertEquals("Message body", new String(message.getPayload()));
	}

	@Test(expected = IllegalStateException.class)
	public void maxBufferSize() throws InterruptedException {
		StompDecoder stompDecoder = new StompDecoder(10);
		String payload = "SEND\na:alpha\n\nMessage body";
		stompDecoder.decode(this.wrap(payload));
	}

	@Test(expected = StompConversionException.class)
	public void invalidFrame() throws InterruptedException {
		StompDecoder stompDecoder = new StompDecoder(10);
		String payload = "FOO\n\n\0";
		stompDecoder.decode(this.wrap(payload));
	}

	@Test
	public void multipleFragments() throws InterruptedException {
		StompDecoder stompDecoder = new StompDecoder(128);
		String payload1 = "SEND\na:alpha\n\nMessage";
		String payload2 = " body\0";
		Message<byte[]> message = stompDecoder.decode(this.wrap(payload1));
		assertNull(message);
		message = stompDecoder.decode(this.wrap(payload2));
		assertNotNull(message);
		assertEquals("Message body", new String(message.getPayload()));
	}

	@Test
	public void reuseNonBuffered() throws InterruptedException {
		StompDecoder stompDecoder = new StompDecoder();
		String payload = "SEND\na:alpha\n\nMessage body\0";
		stompDecoder.decode(this.wrap(payload));

		Message<byte[]> message = stompDecoder.decode(this.wrap(payload));
		assertNotNull(message);
		assertEquals("Message body", new String(message.getPayload()));
	}

	@Test(expected = StompConversionException.class)
	public void reuseBuffered() throws InterruptedException {
		StompDecoder stompDecoder = new StompDecoder();
		String payload1 = "SEND\na:alpha\n\nMessage";
		String payload2 = " body\0";
		stompDecoder.decode(this.wrap(payload1));
		stompDecoder.decode(this.wrap(payload2));
		stompDecoder.decode(this.wrap(payload2));
	}

	private ByteBuffer wrap(String data) {
		return ByteBuffer.wrap(data.getBytes(Charset.forName("UTF-8")));
	}

}
