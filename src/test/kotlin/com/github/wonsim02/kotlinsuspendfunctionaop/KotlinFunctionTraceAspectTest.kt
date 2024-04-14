package com.github.wonsim02.kotlinsuspendfunctionaop

import com.github.wonsim02.kotlinsuspendfunctionaop.annotation.TraceOn
import com.github.wonsim02.kotlinsuspendfunctionaop.util.GlobalTracer
import com.github.wonsim02.kotlinsuspendfunctionaop.util.TraceEvent
import com.github.wonsim02.kotlinsuspendfunctionaop.util.TracingContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.coroutines.Continuation
import kotlin.random.Random
import kotlin.reflect.full.isSuperclassOf
import kotlin.reflect.jvm.javaMethod

class KotlinFunctionTraceAspectTest {

    private inline fun verifyExecutionStartedEvent(
        event: TraceEvent,
        verification: (TraceEvent.ExecutionStarted) -> Unit,
    ) {
        val startedEvent = assertInstanceOf(TraceEvent.ExecutionStarted::class.java, event)
        verification(startedEvent)
    }

    private inline fun <reified T : Any> verifyExecutionSuccessEvent(
        event: TraceEvent,
        verification: (TraceEvent.ExecutionSuccess) -> T,
    ): T {
        val startedEvent = assertInstanceOf(TraceEvent.ExecutionSuccess::class.java, event)
        return verification(startedEvent)
    }

    private inline fun verifyExecutionFailureEvent(
        event: TraceEvent,
        verification: (TraceEvent.ExecutionFailure) -> Unit,
    ) {
        val startedEvent = assertInstanceOf(TraceEvent.ExecutionFailure::class.java, event)
        verification(startedEvent)
    }

    @Test
    fun test(): Unit = runBlocking {
        // given - java methods
        val testSuspendFunctionAsMethod = KotlinFunctionTraceAspectTest::testSuspendFunction.javaMethod!!
        val testSuspendFunction1AsMethod = KotlinFunctionTraceAspectTest::testSuspendFunction1.javaMethod!!
        val testSuspendFunction2AsMethod = KotlinFunctionTraceAspectTest::testSuspendFunction2.javaMethod!!
        val testSuspendFunction3AsMethod = KotlinFunctionTraceAspectTest::testSuspendFunction3.javaMethod!!
        val testSuspendFunction4AsMethod = KotlinFunctionTraceAspectTest::testSuspendFunction4.javaMethod!!

        // when - call testSuspendFunction() and load events
        val returnValue = testSuspendFunction()
        val events = GlobalTracer.listEvents()

        // then - verify number of events
        assertEquals(10, events.size)

        // then - testSuspendFunction() started
        verifyExecutionStartedEvent(events[0]) {
            assertEquals(testSuspendFunctionAsMethod, it.method)
            assertEquals(1, it.args.size)
            assertTrue(Continuation::class.isSuperclassOf(it.args[0].type!!))
        }

        // then - testSuspendFunction1() started
        verifyExecutionStartedEvent(events[1]) {
            assertEquals(testSuspendFunction1AsMethod, it.method)
            assertEquals(1, it.args.size)
            assertTrue(TracingContinuation::class.isSuperclassOf(it.args[0].type!!))
        }

        // then - testSuspendFunction1() success
        val returnValueFromTestSuspendFunction1 = verifyExecutionSuccessEvent(events[2]) {
            assertEquals(testSuspendFunction1AsMethod, it.method)
            assertInstanceOf(Integer::class.java, it.returnValue)
        }

        // then - testSuspendFunction2() started
        verifyExecutionStartedEvent(events[3]) {
            assertEquals(testSuspendFunction2AsMethod, it.method)
            assertEquals(1, it.args.size)
            assertTrue(TracingContinuation::class.isSuperclassOf(it.args[0].type!!))
        }

        // then - testSuspendFunction2() success
        val returnValueFromTestSuspendFunction2 = verifyExecutionSuccessEvent(events[4]) {
            assertEquals(testSuspendFunction2AsMethod, it.method)
            assertInstanceOf(Integer::class.java, it.returnValue)
        }

        // then - testSuspendFunction3() started
        verifyExecutionStartedEvent(events[5]) {
            assertEquals(testSuspendFunction3AsMethod, it.method)
            assertEquals(1, it.args.size)
            assertTrue(TracingContinuation::class.isSuperclassOf(it.args[0].type!!))
        }

        // then - testSuspendFunction3() failure
        verifyExecutionFailureEvent(events[6]) {
            assertEquals(testSuspendFunction3AsMethod, it.method)
            val throwable = assertInstanceOf(RuntimeException::class.java, it.throwable)
            assertEquals("exception from testSuspendFunction3", throwable.message)
        }

        // then - testSuspendFunction4() started
        verifyExecutionStartedEvent(events[7]) {
            assertEquals(testSuspendFunction4AsMethod, it.method)
            assertEquals(1, it.args.size)
            assertTrue(TracingContinuation::class.isSuperclassOf(it.args[0].type!!))
        }

        // then - testSuspendFunction4() failure
        verifyExecutionFailureEvent(events[8]) {
            assertEquals(testSuspendFunction4AsMethod, it.method)
            val throwable = assertInstanceOf(RuntimeException::class.java, it.throwable)
            assertEquals("exception from testSuspendFunction4", throwable.message)
        }

        // then - testSuspendFunction() success
        val returnValueFromTestSuspendFunction = verifyExecutionSuccessEvent(events[9]) {
            assertEquals(testSuspendFunctionAsMethod, it.method)
            assertInstanceOf(Integer::class.java, it.returnValue)
        }

        // then - verify return value
        assertEquals(returnValue, returnValueFromTestSuspendFunction.toInt())
        assertEquals(returnValue, returnValueFromTestSuspendFunction1.toInt() + returnValueFromTestSuspendFunction2.toInt())
    }

    @TraceOn
    private suspend fun testSuspendFunction(): Int {
        val intValue1 = testSuspendFunction1()
        val intValue2 = testSuspendFunction2()
        val intValue3 = runCatching { testSuspendFunction3() }.getOrElse { 0 }
        val intValue4 = runCatching { testSuspendFunction4() }.getOrElse { 0 }
        return intValue1 + intValue2 + intValue3 + intValue4
    }

    @TraceOn
    private suspend fun testSuspendFunction1(): Int = withContext(Dispatchers.IO) {
        delay(100)
        Random.nextInt()
    }

    @Suppress("RedundantSuspendModifier")
    @TraceOn
    private suspend fun testSuspendFunction2(): Int {
        return Random.nextInt()
    }

    @TraceOn
    private suspend fun testSuspendFunction3(): Int {
        delay(100)
        throw RuntimeException("exception from testSuspendFunction3")
    }

    @Suppress("RedundantSuspendModifier")
    @TraceOn
    private suspend fun testSuspendFunction4(): Int {
        throw RuntimeException("exception from testSuspendFunction4")
    }
}
