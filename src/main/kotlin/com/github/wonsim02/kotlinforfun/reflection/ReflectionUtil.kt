package com.github.wonsim02.kotlinforfun.reflection

import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import kotlin.reflect.KClass
import kotlin.reflect.KTypeParameter
import kotlin.reflect.KTypeProjection
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.kotlinFunction

/**
 * @see <a href="https://blog.kotlin-academy.com/creating-a-random-instance-of-any-class-in-kotlin-b6168655b64a">
 *     Creating a random instance of any class in Kotlin
 *     </a>
 */
object ReflectionUtil {

    private val classLoader: ClassLoader by lazy { this::class.java.classLoader }

    private class InnerInvocationHandler(
        private val typeProjectionsByName: Map<String, KTypeProjection>,
    ) : InvocationHandler {

        override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any? {
            val kotlinFunc = method.kotlinFunction ?: return null
            val returnType = kotlinFunc.returnType

            return if (returnType.isMarkedNullable) {
                null
            } else {
                val returnTypeClazz = when (val returnTypeClassifier = returnType.classifier) {
                    is KClass<*>
                    -> returnTypeClassifier
                    is KTypeParameter
                    -> typeProjectionsByName[returnTypeClassifier.name]!!
                        .type!!
                        .classifier as KClass<*>
                    else -> return null
                }

                makeDefaultInstance(returnTypeClazz)
            }
        }
    }

    fun makeDefaultInstance(
        clazz: KClass<*>,
        vararg typeProjections: KTypeProjection,
    ): Any {
        handlePrimitive(clazz)?.let { return it }

        val typeParams = clazz.typeParameters
        assert(typeProjections.size == typeParams.size)
        val typeProjectionsByName = typeProjections
            .zip(typeParams)
            .associate { (typeProjection, typeParam) -> typeParam.name to typeProjection }

        val constructors = clazz
            .constructors
            .filter { it.isAccessible }
            .sortedBy { it.parameters.size }

        for (constructor in constructors) {
            try {
                val arguments = constructor
                    .parameters
                    .asSequence()
                    .filterNot { it.isOptional }
                    .associateWith { parameter ->
                        val paramType = parameter.type
                        if (paramType.isMarkedNullable) return@associateWith null

                        val paramTypeClazz = when (val paramTypeClassifier = paramType.classifier) {
                            is KClass<*>
                            -> paramTypeClassifier
                            is KTypeParameter
                            -> typeProjectionsByName[paramTypeClassifier.name]!!
                                .type!!
                                .classifier as KClass<*>
                            else -> throw RuntimeException()
                        }

                        makeDefaultInstance(paramTypeClazz, *paramType.arguments.toTypedArray())
                    }

                return constructor.callBy(arguments)
            } catch (t: Throwable) {
                continue
            }
        }

        for (sealedSubclass in clazz.sealedSubclasses) {
            try {
                makeDefaultInstance(sealedSubclass, *typeProjections)
            } catch (t: Throwable) {
                continue
            }
        }

        if (clazz.java.isInterface) {
            return Proxy.newProxyInstance(
                classLoader,
                Array(1) { clazz.java },
                InnerInvocationHandler(typeProjectionsByName),
            )
        }

        throw RuntimeException("Failed to construct new instance of type=$clazz.")
    }

    /**
     * @see kotlin.reflect.jvm.internal.defaultPrimitiveValue
     */
    private fun handlePrimitive(clazz: KClass<*>): Any? {
        return when (clazz) {
            // Java primitives
            Boolean::class -> false
            Char::class -> 0.toChar()
            Byte::class -> 0.toByte()
            Short::class -> 0.toShort()
            Int::class -> 0
            Float::class -> 0f
            Long::class -> 0L
            Double::class -> 0.0
            // etc
            String::class -> ""
            else -> null
        }
    }
}
