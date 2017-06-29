package org.springframework.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

import kotlin.reflect.KFunction;
import kotlin.reflect.KParameter;
import kotlin.reflect.jvm.ReflectJvmMapping;

import org.springframework.core.MethodParameter;

public abstract class KotlinUtils {

	private static final boolean kotlinPresent =
	ClassUtils.isPresent("kotlin.Unit", KotlinUtils.class.getClassLoader());

	public static boolean isKotlinClass(Class<?> clazz) {
		for (Annotation annotation : clazz.getDeclaredAnnotations()) {
			if (annotation.annotationType().getName().equals("kotlin.Metadata")) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Check whether the specified {@link MethodParameter} represents a nullable Kotlin type or not.
	 */
	public static boolean isNullable(MethodParameter param) {
		return kotlinPresent && KotlinDelegate.isNullable(param);
	}

	public static Constructor<?> getPrimaryConstructor(Class<?> clazz) {
		return KotlinUtilsKt.getPrimaryConstructor(clazz);
	}

	/**
	 * Inner class to avoid a hard dependency on Kotlin at runtime.
	 */
	private static class KotlinDelegate {

		/**
		 * Check whether the specified {@link MethodParameter} represents a nullable Kotlin type or not.
		 */
		public static boolean isNullable(MethodParameter param) {
			if (isKotlinClass(param.getContainingClass())) {
				Method method = param.getMethod();
				Constructor<?> ctor = param.getConstructor();
				int index = param.getParameterIndex();
				if (method != null && index == -1) {
					KFunction<?> function = ReflectJvmMapping.getKotlinFunction(method);
					return (function != null && function.getReturnType().isMarkedNullable());
				}
				else {
					KFunction<?> function = null;
					if (method != null) {
						function = ReflectJvmMapping.getKotlinFunction(method);
					}
					else if (ctor != null) {
						function = ReflectJvmMapping.getKotlinFunction(ctor);
					}
					if (function != null) {
						List<KParameter> parameters = function.getParameters();
						return parameters
								.stream()
								.filter(p -> KParameter.Kind.VALUE.equals(p.getKind()))
								.collect(Collectors.toList())
								.get(index)
								.getType()
								.isMarkedNullable();
					}
				}
			}

			return false;
		}
	}

}
