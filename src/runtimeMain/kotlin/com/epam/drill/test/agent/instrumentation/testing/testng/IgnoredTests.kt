/**
 * Copyright 2020 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.epam.drill.test.agent.instrumentation.testing.testng

import com.epam.drill.test.agent.*
import com.epam.drill.test.agent.instrumentation.*
import javassist.*
import org.objectweb.asm.*
import java.security.*

// Only for testng 7.4.0
@Suppress("unused")
object IgnoredTests : AbstractTestStrategy() {
    override val id: String
        get() = "testng"
    private const val IIgnoreAnnotation = "org.testng.annotations.IIgnoreAnnotation"

    override fun permit(classReader: ClassReader): Boolean {
        return classReader.className == "org/testng/internal/annotations/AnnotationHelper"
    }

    /**
     * Support 7.4.0 testng @Ignore annotation
     */
    override fun instrument(
        ctClass: CtClass,
        pool: ClassPool,
        classLoader: ClassLoader?,
        protectionDomain: ProtectionDomain?,
    ): ByteArray? {
        pool.getOrNull(IIgnoreAnnotation)?.also {
            ctClass.getMethod(
                "isAnnotationPresent",
                "(Lorg/testng/internal/annotations/IAnnotationFinder;Ljava/lang/reflect/Method;Ljava/lang/Class;)Z"
            ).insertAfter(
                """ 
            if ($3 == $IIgnoreAnnotation.class && ${'$'}_) {
                ${TestListener::class.java.name}.INSTANCE.${TestListener::testIgnored.name}("${TestNGStrategy.engineSegment}", $2.getDeclaringClass().getName(), $2.getName());
            }
        """.trimIndent()
            )
        }
        return ctClass.toBytecode()
    }
}
