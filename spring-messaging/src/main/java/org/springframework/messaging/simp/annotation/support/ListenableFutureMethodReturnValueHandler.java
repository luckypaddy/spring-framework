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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.HandlerMethod;
import org.springframework.messaging.handler.invocation.HandlerMethodExceptionProcessor;
import org.springframework.messaging.handler.invocation.HandlerMethodReturnValueHandler;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

/**
 * A {@link HandlerMethodReturnValueHandler} for handling {@link ListenableFuture} return
 * values. It is mainly a wrapper that will call the relevant return value handler like
 * {@link SendToMethodReturnValueHandler} or {@link SubscriptionMethodReturnValueHandler}
 * when the callback will be completed. In case of failure, the relevant exception handler
 * will be used.
 * 
 * @author Sebastien Deleuze
 * @since 4.2
 * @see ListenableFutureMethodReturnValueHandler
 */
public class ListenableFutureMethodReturnValueHandler implements HandlerMethodReturnValueHandler {
	
	private final Log logger = LogFactory.getLog(getClass());
	
	private final List<HandlerMethodReturnValueHandler> handlers;
	
	private final HandlerMethodExceptionProcessor exceptionProcessor;

	public ListenableFutureMethodReturnValueHandler(List<HandlerMethodReturnValueHandler> handlers, HandlerMethodExceptionProcessor exceptionProcessor) {
		this.handlers = handlers; 
		this.exceptionProcessor  = exceptionProcessor;
	}

	@Override
	public boolean supportsReturnType(MethodParameter returnType) {
		return ListenableFuture.class.isAssignableFrom(returnType.getParameterType());
	}

	@Override
	public void handleReturnValue(Object returnValue, final HandlerMethod handlerMethod, final Message<?> message) throws Exception {
		MethodParameter returnType = handlerMethod.getReturnType();
		ListenableFuture<?> future = (ListenableFuture<?>) returnValue;
		for (final HandlerMethodReturnValueHandler handler : this.handlers) {
			if ((handler != this) && handler.supportsReturnType(returnType)) {
				future.addCallback(new ListenableFutureCallback<Object>() {
					@Override
					public void onSuccess(Object result) {
						try {
							handler.handleReturnValue(result, handlerMethod, message);
						} catch(Exception ex) {
							onFailure(ex);
						}
					}
					@Override
					public void onFailure(Throwable ex) {
						if (!(ex instanceof Exception)) {
							logger.error("Error while processing message " + message, ex);
						}
						else {
							exceptionProcessor.processHandlerMethodException(handlerMethod, (Exception)ex, message);
						}
					}
				});
				
			}
		}
	}

}
