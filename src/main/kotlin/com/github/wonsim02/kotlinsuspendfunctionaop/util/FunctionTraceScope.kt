package com.github.wonsim02.kotlinsuspendfunctionaop.util

import java.lang.reflect.Method

/**
 * 함수 실행 1회에 대해서 시작 및 종료를 추적하는 유틸.
 * - 함수 실행 시작 이벤트는 [FunctionTraceScope]가 생성될 때 [GlobalTracer]에 추가된다.
 * - 함수 실행 성공 이벤트는 [endWithReturnValue] 호출 시 [GlobalTracer]에 추가된다.
 * - 함수 실행 실패 이벤트는 [endWithThrowable] 호출 시 [GlobalTracer]에 추가된다.
 */
class FunctionTraceScope(private val method: Method, args: Array<*>) {

    init {
        GlobalTracer.addStartedEvent(method, args)
    }

    private var traceEnded: Boolean = false

    fun endWithReturnValue(returnValue: Any?) {
        if (!traceEnded) {
            traceEnded = true
            GlobalTracer.addSuccessEvent(method, returnValue)
        }
    }

    fun endWithThrowable(throwable: Throwable) {
        if (!traceEnded) {
            traceEnded = true
            GlobalTracer.addFailureEvent(method, throwable)
        }
    }

    override fun toString(): String {
        val superToString = super.toString()
        return "$superToString(method=$method)"
    }
}
