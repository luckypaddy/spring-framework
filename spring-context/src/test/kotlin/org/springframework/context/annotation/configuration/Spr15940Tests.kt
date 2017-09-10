/*
 * Copyright 2002-2017 the original author or authors.
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
package org.springframework.context.annotation.configuration

import org.junit.Assert.*
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * @author Sebastien Deleuze
 */
class Spr15940Tests {

	@Test // Pass
	fun autowireMutableCollection() {
		val ctx = AnnotationConfigApplicationContext()
		ctx.register(Config::class.java)
		ctx.refresh()
		val bean = ctx.getBean(AutowiredBean::class.java)
		assertEquals(4, bean.mutableCollection.size)
	}

	@Test // Fail
	fun autowireCollection() {
		val ctx = AnnotationConfigApplicationContext()
		ctx.register(Config::class.java)
		ctx.refresh()
		val bean = ctx.getBean(AutowiredBean::class.java)
		assertEquals(4, bean.collection.size)
	}

}

interface FirstInterface
interface SecondInterface
interface ThirdInterface

class Bean1 : FirstInterface, SecondInterface, ThirdInterface
class Bean2 : FirstInterface, SecondInterface, ThirdInterface
class Bean3 : FirstInterface, SecondInterface, ThirdInterface
class Bean4 : FirstInterface, SecondInterface, ThirdInterface

class AutowiredBean {

	@Autowired
	lateinit var mutableCollection: MutableCollection<SecondInterface>

	@Autowired
	lateinit var collection: Collection<SecondInterface>

}

@Configuration
open class Config {
	@Bean open fun bean1() : Bean1 = Bean1()
	@Bean open fun bean2() : FirstInterface = Bean2()
	@Bean open fun bean3() : SecondInterface = Bean3()
	@Bean open fun bean4() : ThirdInterface = Bean4()
	@Bean open fun autowiredBean() = AutowiredBean()
}