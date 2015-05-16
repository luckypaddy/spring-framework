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

package org.springframework.messaging.simp.annotation.support;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.BDDMockito.given;
import org.mockito.Captor;
import static org.mockito.Matchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import org.mockito.MockitoAnnotations;

import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.HandlerMethod;
import org.springframework.messaging.handler.invocation.HandlerMethodExceptionProcessor;
import org.springframework.messaging.handler.invocation.HandlerMethodReturnValueHandler;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

/**
 * Test fixture for {@link CompletableFutureMethodReturnValueHandler}.
 *
 * @author Sebastien Deleuze
 */
public class CompletableFutureMethodReturnValueHandlerTests {

	private static final String PAYLOAD = "payload";


	private CompletableFutureMethodReturnValueHandler handler;

	@Mock
	private HandlerMethodReturnValueHandler returnValueHandler;

	@Mock
	private HandlerMethodExceptionProcessor exceptionProcessor;

	@Captor
	private ArgumentCaptor<Object> returnValueCaptor;

	@Captor
	private ArgumentCaptor<Exception> exceptionCaptor;

	private HandlerMethod valueHandlerMethod;
	private HandlerMethod futureHandlerMethod;

	@Before
	public void setup() throws Exception {
		MockitoAnnotations.initMocks(this);
		this.handler = new CompletableFutureMethodReturnValueHandler(Arrays.asList(this.returnValueHandler), this.exceptionProcessor);
		Method method = this.getClass().getDeclaredMethod("handleValue");
		this.valueHandlerMethod = new HandlerMethod(this, method);
		method = this.getClass().getDeclaredMethod("handleFuture");
		this.futureHandlerMethod = new HandlerMethod(this, method);
	}

	@Test
	public void supportsReturnType() {
		given(this.returnValueHandler.supportsReturnType(any(MethodParameter.class))).willReturn(true);
		assertFalse(this.handler.supportsReturnType(this.valueHandlerMethod.getReturnType()));
		assertTrue(this.handler.supportsReturnType(this.futureHandlerMethod.getReturnType()));
	}

	@Test
	public void handleSuccess() throws Exception {
		given(this.returnValueHandler.supportsReturnType(any(MethodParameter.class))).willReturn(true);

		Message<?> inputMessage = MessageBuilder.createMessage(new byte[0], SimpMessageHeaderAccessor.create().getMessageHeaders());
		CompletableFuture<String> future = handleFuture();
		this.handler.handleReturnValue(future, this.futureHandlerMethod, inputMessage);
		future.complete(PAYLOAD);

		verify(this.returnValueHandler).handleReturnValue(this.returnValueCaptor.capture(), any(HandlerMethod.class), any(Message.class));
		Object returnValue = this.returnValueCaptor.getValue();
		assertNotNull(returnValue);
		assertEquals(PAYLOAD, returnValue);
	}

	@Test
	public void handleFailure() throws Exception {
		given(this.returnValueHandler.supportsReturnType(any(MethodParameter.class))).willReturn(true);

		Message<?> inputMessage = MessageBuilder.createMessage(new byte[0], SimpMessageHeaderAccessor.create().getMessageHeaders());
		CompletableFuture<String> future = handleFuture();
		this.handler.handleReturnValue(future, this.futureHandlerMethod, inputMessage);
		future.completeExceptionally(new Exception("foo"));

		verify(this.exceptionProcessor).processHandlerMethodException(any(HandlerMethod.class), this.exceptionCaptor.capture(), any(Message.class));
		Exception ex = this.exceptionCaptor.getValue();
		assertNotNull(ex);
		assertEquals("foo", ex.getMessage());
	}

	@SuppressWarnings("unused")
	public String handleValue() {
		return PAYLOAD;
	}

	@SuppressWarnings("unused")
	public CompletableFuture<String> handleFuture() {
		return new CompletableFuture<>();
	}

}
