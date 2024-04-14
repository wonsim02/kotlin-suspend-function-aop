package com.github.wonsim02.kotlinsuspendfunctionaop.util

import org.aspectj.lang.ProceedingJoinPoint
import java.lang.reflect.Method
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext

/**
 * suspend 함수가 최종 결과값을 계산하여 suspend 함수를 호출한 부분으로 결과값을 전달할 때 [FunctionTraceScope]을 종료하는 컨티뉴에이션.
 */
class TracingContinuation private constructor(
    private val original: Continuation<Any?>,
) : Continuation<Any?> {

    override val context: CoroutineContext
        get() = original.context

    private lateinit var _scope: FunctionTraceScope
    val scope: FunctionTraceScope get() = _scope

    override fun resumeWith(result: Result<Any?>) {
        result.fold(scope::endWithReturnValue, scope::endWithThrowable)
        original.resumeWith(result)
    }

    override fun toString(): String {
        val superToString = super.toString()
        return if (this::_scope.isInitialized) {
            "$superToString(original=$original) with scope=$scope"
        } else {
            "$superToString(original=$original)"
        }
    }

    companion object {

        /**
         * suspend 함수를 컴파일할 때 생성되는 대상 suspend 함수 전용 [Continuation]인지 검사하여 [TracingContinuation]으로 감쌀 수 있는지
         * 파악한다. (이름 패턴으로 검사하는 것이라서 안전하지는 않음)
         * @see <a href="https://github.com/JetBrains/kotlin/blob/master/compiler/backend/src/org/jetbrains/kotlin/codegen/coroutines/coroutines-codegen.md">
         *     Coroutines Codegen
         *     </a>
         */
        private fun isWrappable(
            cont: Continuation<Any?>,
            method: Method,
        ): Boolean {
            // 이미 `TracingContinuation`이면 다시 감싸지 않는다.
            if (cont is TracingContinuation) return false

            val contType = cont::class.java.name
            val pattern = method.declaringClass.name + "$" + method.name
            return !contType.startsWith(pattern)
        }

        /**
         * suspend 함수에 대하여 특정 조건을 만족할 때 마지막 [Continuation] 변수를 [TracingContinuation]으로 변경한다.
         * [ProceedingJoinPoint.getArgs]의 구현체에서 배열 복사를 진행하기 때문에 마지막 변수 검사 및 변경을 동일한 함수 내에서 진행한다.
         * @see org.aspectj.runtime.reflect.JoinPointImpl.getArgs
         */
        fun replaceContinuationArg(
            pjp: ProceedingJoinPoint,
            method: Method,
        ): Pair<Array<out Any?>, TracingContinuation>? {
            val args = pjp.args
            if (args.isEmpty()) return null

            @Suppress("UNCHECKED_CAST")
            val cont = args[args.size - 1] as? Continuation<Any?>
                ?: return null

            return if (isWrappable(cont, method)) {
                val newCont = TracingContinuation(cont)
                args[args.size - 1] = newCont

                val scope = FunctionTraceScope(method, args)
                newCont._scope = scope

                args to newCont
            } else {
                null
            }
        }
    }
}
