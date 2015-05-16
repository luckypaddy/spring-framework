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

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.MethodParameter;
import org.springframework.lang.UsesJava8;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.HandlerMethod;
import org.springframework.messaging.handler.invocation.HandlerMethodExceptionProcessor;
import org.springframework.messaging.handler.invocation.HandlerMethodReturnValueHandler;

/**
 * A {@link HandlerMethodReturnValueHandler} for handling (like {@link CompletableFuture}
 * return values. It is mainly a wrapper that will call the relevant return value handler like
 * {@link SendToMethodReturnValueHandler} or {@link SubscriptionMethodReturnValueHandler}
 * when the callback will be completed. In case of failure, the relevant exception handler
 * will be used.
 * 
 * @author Sebastien Deleuze
 * @since 4.2
 * @see ListenableFutureMethodReturnValueHandler
 */
@UsesJava8
public class CompletableFutureMethodReturnValueHandler implements HandlerMethodReturnValueHandler {

	private final Log logger = LogFactory.getLog(getClass());

	private final List<HandlerMethodReturnValueHandler> handlers;

	private final HandlerMethodExceptionProcessor exceptionProcessor;

	public CompletableFutureMethodReturnValueHandler(List<HandlerMethodReturnValueHandler> handlers, HandlerMethodExceptionProcessor exceptionProcessor) {
		this.handlers = handlers;
		this.exceptionProcessor  = exceptionProcessor;
	}

	@Override
	public boolean supportsReturnType(MethodParameter returnType) {
		return CompletableFuture.class.isAssignableFrom(returnType.getParameterType());
	}

	@Override
	public void handleReturnValue(Object returnValue, final HandlerMethod handlerMethod, final Message<?> message) throws Exception {
		MethodParameter returnType = handlerMethod.getReturnType();
		@SuppressWarnings("unchecked")
		final CompletableFuture<Object> future = (CompletableFuture<Object>) returnValue;
		for (final HandlerMethodReturnValueHandler handler : this.handlers) {
			if ((handler != this) && handler.supportsReturnType(returnType)) {
				future.thenAccept(new Consumer<Object>() {
					@Override
					public void accept(Object result) {
						try {
							handler.handleReturnValue(result, handlerMethod, message);
						}
						catch(Exception ex) {
							future.completeExceptionally(ex);
						}
					}
				});
				future.exceptionally(new Function<Throwable, Object>() {
					@Override
					public Object apply(Throwable ex) {
						if (!(ex instanceof Exception)) {
							logger.error("Error while processing message " + message, ex);
						}
						else {
							exceptionProcessor.processHandlerMethodException(handlerMethod, (Exception)ex, message);
						}
						return null;
					}
				});
			}
		}
	}

}
