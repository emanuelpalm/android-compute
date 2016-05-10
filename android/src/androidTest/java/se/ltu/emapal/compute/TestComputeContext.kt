package se.ltu.emapal.compute

import android.support.test.runner.AndroidJUnit4
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import se.ltu.emapal.compute.util.Result
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class TestComputeContext {
    @Test
    fun shouldRegisterLambdaAndProcessBatch() {
        val computeContext = ComputeContext()

        val program = "" +
                "lcm:register(function (batch)\n" +
                "  return batch:upper()\n" +
                "end)"

        val registerResult = computeContext.register(ComputeLambda(1, program))
        if (registerResult is Result.Failure) {
            Assert.fail(registerResult.error.message)
        }
        val processResult = computeContext.process(ComputeBatch(1, 100, "hello".toByteArray()))
        when (processResult) {
            is Result.Success -> Assert.assertEquals(ComputeBatch(1, 100, "HELLO".toByteArray()), processResult.value)
            is Result.Failure -> Assert.fail(processResult.error.message)
        }
    }

    @Test
    fun shouldRegisterBadLambdaAndReturnError() {
        val computeContext = ComputeContext()

        var registerResult0 = computeContext.register(ComputeLambda(1, "donut()"))
        when (registerResult0) {
            is Result.Success -> Assert.fail("Expected error not returned.")
            is Result.Failure -> Assert.assertNotEquals(0, registerResult0.error.code)
        }

        var registerResult1 = computeContext.register(ComputeLambda(1, "Å^()["))
        when (registerResult1) {
            is Result.Success -> Assert.fail("Expected error not returned.")
            is Result.Failure -> Assert.assertNotEquals(0, registerResult1.error.code)
        }
    }

    @Test
    fun shouldPublishLogCalls() {
        val computeContext = ComputeContext()

        val logCounter = AtomicInteger(0)
        computeContext.WhenLogEntry().let {
            it.skip(0).take(1)
                    .subscribe {
                        logCounter.incrementAndGet()

                        Assert.assertNotEquals(0, it.timestamp.toMilliseconds())
                        Assert.assertEquals(12, it.lambdaId)
                        Assert.assertEquals(0, it.batchId)
                        Assert.assertEquals("Hello, register!", it.message)
                    }

            it.skip(1).take(1)
                    .subscribe {
                        logCounter.incrementAndGet()

                        Assert.assertNotEquals(0, it.timestamp.toMilliseconds())
                        Assert.assertEquals(12, it.lambdaId)
                        Assert.assertEquals(34, it.batchId)
                        Assert.assertEquals("Hello, process!", it.message)
                    }
        }

        val program = "" +
                "lcm:log(\"Hello, register!\")\n" +
                "lcm:register(function (batch)\n" +
                "  lcm:log(\"Hello, process!\")\n" +
                "  return batch:upper()\n" +
                "end)"

        val registerResult = computeContext.register(ComputeLambda(12, program))
        if (registerResult is Result.Failure) {
            Assert.fail(registerResult.error.message)
        }
        val processResult = computeContext.process(ComputeBatch(12, 34, "hello".toByteArray()))
        if (processResult is Result.Failure) {
            Assert.fail(processResult.error.message)
        }
        Assert.assertEquals(2, logCounter.get())
    }
}