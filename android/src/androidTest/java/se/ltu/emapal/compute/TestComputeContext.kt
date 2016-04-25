package se.ltu.emapal.compute

import android.support.test.runner.AndroidJUnit4
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import se.ltu.emapal.compute.util.Result

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
}