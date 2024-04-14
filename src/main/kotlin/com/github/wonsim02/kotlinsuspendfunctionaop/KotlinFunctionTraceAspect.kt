package com.github.wonsim02.kotlinsuspendfunctionaop

import com.github.wonsim02.kotlinsuspendfunctionaop.annotation.TraceOn
import com.github.wonsim02.kotlinsuspendfunctionaop.util.FunctionTraceScope
import com.github.wonsim02.kotlinsuspendfunctionaop.util.TracingContinuation
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.reflect.jvm.kotlinFunction

/**
 * [TraceOn] 어노테이션에 대한 Aspect.
 */
@Aspect
class KotlinFunctionTraceAspect {

    /**
     * [TraceOn] 어노테이션이 첨부된 함수 실행 시 호출된다.
     */
    @Around("@annotation($ANNOTATION_NAME) && execution(* *(..))")
    fun traceOn(pjp: ProceedingJoinPoint): Any? {
        val method = (pjp.signature as MethodSignature).method
        val kotlinFunc = method.kotlinFunction
            // 코틀린 함수가 아니면 아무런 추가 로직을 실행하지 않는다.
            ?: return pjp.proceed()

        // suspend 함수에 대한 로직 분기
        if (kotlinFunc.isSuspend) {
            // suspend 함수가 컴파일되면 Continuation 타입의 변수가 추가되는 형식이 된다.
            // 해당 Continuation 변수를 TracingContinuation 클래스로 감싸서 기존 Continuation 변수를 대체한다.
            val (newArgs, newCont) = TracingContinuation
                .replaceContinuationArg(pjp, method)
                // replaceContinuationArg() 함수에서 null이 반환되는 경우는
                // 마지막 변수를 TracingContinuation 클래스로 감쌀 수 없는 경우
                ?: return pjp.proceed()

            return try {
                pjp.proceed(newArgs).also { returnValue ->
                    // 컨티뉴에이션의 resumeWith()을 통하지 않고 결과값을 반환하는 경우 endWithReturnValue() 직접 호출
                    if (returnValue !== COROUTINE_SUSPENDED) {
                        newCont.scope.endWithReturnValue(returnValue)
                    }
                }
            } catch (throwable: Throwable) {
                // 컨티뉴에이션의 resumeWith()을 통하지 않고 예외를 발생시키는 경우 endWithThrowable() 직접 호출
                newCont.scope.endWithThrowable(throwable)
                throw throwable
            }
        }

        // suspend 함수가 아닌 경우
        val scope = FunctionTraceScope(method, pjp.args)
        return try {
            // 결과값에 대하여 endWithReturnValue() 호출
            pjp.proceed().also(scope::endWithReturnValue)
        } catch (throwable: Throwable) {
            // 예외에 대하여 endWithThrowable() 호출
            scope.endWithThrowable(throwable)
            throw throwable
        }
    }

    companion object {

        const val ANNOTATION_NAME = "com.github.wonsim02.kotlinsuspendfunctionaop.annotation.TraceOn"

        init {
            val annotationClass = Class.forName(ANNOTATION_NAME)
            assert(annotationClass == TraceOn::class.java)
        }
    }
}
