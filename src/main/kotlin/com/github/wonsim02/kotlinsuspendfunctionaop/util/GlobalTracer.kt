package com.github.wonsim02.kotlinsuspendfunctionaop.util

import java.lang.reflect.Method
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * 함수 실행 시작 및 종료 이벤트를 순서대로 적재하는 유틸.
 */
object GlobalTracer {

    private val traceEvents = ConcurrentLinkedQueue<TraceEvent>()

    fun addStartedEvent(method: Method, args: Array<*>) {
        doAddEvent {
            TraceEvent.ExecutionStarted(
                method = method,
                threadName = it,
                args = args,
            )
        }
    }

    fun addSuccessEvent(method: Method, returnValue: Any?) {
        doAddEvent {
            TraceEvent.ExecutionSuccess(
                method = method,
                threadName = it,
                returnValue = returnValue,
            )
        }
    }

    fun addFailureEvent(method: Method, throwable: Throwable) {
        doAddEvent {
            TraceEvent.ExecutionFailure(
                method = method,
                threadName = it,
                throwable = throwable,
            )
        }
    }

    fun listEvents(): List<TraceEvent> {
        return traceEvents.toList()
    }

    private inline fun doAddEvent(createEvent: (String) -> TraceEvent) {
        traceEvents.add(createEvent(Thread.currentThread().name))
    }
}
