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

package org.springframework.core;

import java.lang.reflect.Type;

/**
 * POJO wrapper that also contains {@link Type} information, useful for avoiding to lose
 * parametrized type information.
 *
 * @author Sebastien Deleuze
 * @since 4.2
 * @see ParameterizedTypeAware
 * @see ParameterizedTypeReference
 */
public class ParameterizedTypeValue {

	private Object value;

	private Type type;


	/**
	 * Create a new instance wrapping the given POJO.
	 * @param value the Object to be serialized
	 */
	public ParameterizedTypeValue(Object value) {
		this.value = value;
	}

	/**
	 * Create a new instance wrapping the given POJO.
	 * @param value the Object to be serialized
	 * @param type the Type to be retained
	 */
	public ParameterizedTypeValue(Object value, Type type) {
		this.type = type;
		this.value = value;
	}

	/**
	 * Modify the type to retain.
	 */
	public void setType(Type type) {
		this.type = type;
	}

	/**
	 * Return the type that has be retained.
	 */
	public Type getType() {
		return type;
	}

	/**
	 * Return the wrapped POJO.
	 */
	public Object getValue() {
		return value;
	}

	/**
	 * Modify the wrapped POJO.
	 */
	public void setValue(Object value) {
		this.value = value;
	}

}
