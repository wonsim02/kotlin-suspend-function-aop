package com.github.wonsim02.kotlinsuspendfunctionaop.util

import java.lang.reflect.Method
import kotlin.reflect.KClass

/**
 * 함수 실행 시작 및 종료에 대한 이벤트.
 */
sealed class TraceEvent {

    abstract val method: Method
    abstract val threadName: String

    class ExecutionStarted(
        override val method: Method,
        override val threadName: String,
        val args: List<Arg>,
    ) : TraceEvent() {

        constructor(
            method: Method,
            threadName: String,
            args: Array<*>,
        ) : this(
            method = method,
            threadName = threadName,
            args = args.map { Arg(it) },
        )

        data class Arg(
            val stringExpression: String,
            val type: KClass<*>?,
        ) {
            constructor(arg: Any?) : this(
                stringExpression = arg.toString(),
                type = arg?.let { it::class },
            )
        }
    }

    class ExecutionSuccess(
        override val method: Method,
        override val threadName: String,
        val returnValue: Any?,
    ) : TraceEvent()

    class ExecutionFailure(
        override val method: Method,
        override val threadName: String,
        val throwable: Throwable,
    ) : TraceEvent()
}
