package org.springframework.util

import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaConstructor

fun getPrimaryConstructor(clazz: Class<*>) = clazz.kotlin.primaryConstructor?.javaConstructor

