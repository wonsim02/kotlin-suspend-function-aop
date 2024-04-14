package com.github.wonsim02.kotlinsuspendfunctionaop.annotation

import com.github.wonsim02.kotlinsuspendfunctionaop.util.GlobalTracer

/**
 * 코틀린 함수에 해당 어노테이션을 추가하면 함수 시작 및 종료에 대한 이벤트가 [GlobalTracer]에 추가된다.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class TraceOn
