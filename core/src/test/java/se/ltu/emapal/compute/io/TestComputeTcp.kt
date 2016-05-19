package se.ltu.emapal.compute.io

import org.junit.Assert
import org.junit.Test
import se.ltu.emapal.compute.ComputeBatch
import se.ltu.emapal.compute.ComputeError
import se.ltu.emapal.compute.ComputeLambda
import se.ltu.emapal.compute.ComputeLogEntry
import se.ltu.emapal.compute.util.time.Duration
import java.net.InetSocketAddress
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class TestComputeTcp {
    @Test
    fun shouldConnectToServerAndSendMessages() {
        // Keeps track of significant events.
        val semaphore = Semaphore(0)

        val serviceReceivedBatches = AtomicInteger(0)
        val serviceReceivedErrors = AtomicInteger(0)
        val serviceReceivedLogEntries = AtomicInteger(0)
        val serviceStatusConnected = AtomicBoolean(false)
        val serviceStatusTerminated = AtomicBoolean(false)

        val clientReceivedBatches = AtomicInteger(0)
        val clientReceivedLambdas = AtomicInteger(0)
        val clientStatusConnecting = AtomicBoolean(false)
        val clientStatusConnected = AtomicBoolean(false)
        val clientStatusTerminated = AtomicBoolean(false)

        ComputeServiceTcpListener(
                hostAddress = InetSocketAddress(62001),
                timeout = Duration.ofSeconds(10000),
                executorDelay = Duration.ofMilliseconds(5),
                executorInterval = Duration.ofMilliseconds(1)
        ).use {
            it.whenException.subscribe {
                Assert.fail(it.message)
            }
            it.whenConnect.subscribe() { service ->
                service.whenBatch.subscribe {
                    Assert.assertEquals("Service: Expected only one batch.", serviceReceivedBatches.andIncrement, 0)
                    Assert.assertEquals(ComputeBatch(1, 100, "HELLO".toByteArray()), it)
                    semaphore.release()
                }
                service.whenError.subscribe {
                    Assert.assertEquals("Service: Expected only one error.", serviceReceivedErrors.andIncrement, 0)
                    Assert.assertEquals(12345, it.code)
                    Assert.assertEquals("bad", it.message)
                    semaphore.release()
                }
                service.whenLogEntry.subscribe {
                    Assert.assertEquals("Service:Expected only one log entry.", serviceReceivedLogEntries.andIncrement, 0)
                    Assert.assertNotNull(it.timestamp)
                    Assert.assertEquals(1, it.lambdaId)
                    Assert.assertEquals(100, it.batchId)
                    Assert.assertEquals("world!", it.message)
                    semaphore.release()
                }
                service.whenException.subscribe {
                    Assert.fail(it.message)
                }
                service.whenStatus.subscribe {
                    when(it) {
                        ComputeServiceStatus.CONNECTED -> {
                            if (!serviceStatusConnected.compareAndSet(false, true)) {
                                Assert.fail("Service: Already connected!")
                            }
                            service.submit(ComputeLambda(1, "" +
                                    "lcm:register(function (batch)\n" +
                                    "  batch:upper()\n" +
                                    "  lcm:log(\"world!\")\n" +
                                    "end)"))
                            service.submit(ComputeBatch(1, 100, "hello".toByteArray()))
                            semaphore.release()
                        }
                        ComputeServiceStatus.DISRUPTED -> Assert.fail("Service: Disrupted unexpectedly.")
                        ComputeServiceStatus.TERMINATED -> {
                            if (!serviceStatusTerminated.compareAndSet(false, true)) Assert.fail("Service: Already terminated!")
                            semaphore.release()
                        }
                        else -> Assert.fail("Service: Unexpected status $it.")
                    }
                }
            }

            ComputeClientTcp(
                    address = InetSocketAddress(62001),
                    timeout = Duration.ofSeconds(10000),
                    executorDelay = Duration.ofMilliseconds(10),
                    executorInterval = Duration.ofMilliseconds(1)
            ).use { client ->
                client.whenBatch().subscribe {
                    Assert.assertEquals("Client: Expected only one batch.", clientReceivedBatches.andIncrement, 0)
                    Assert.assertEquals(ComputeBatch(1, 100, "hello".toByteArray()), it)
                    semaphore.release()
                }
                client.whenLambda().subscribe {
                    Assert.assertEquals("Client: Expected only one lambda.", clientReceivedLambdas.andIncrement, 0)
                    Assert.assertEquals(ComputeLambda(1, "" +
                            "lcm:register(function (batch)\n" +
                            "  batch:upper()\n" +
                            "  lcm:log(\"world!\")\n" +
                            "end)"), it)
                    semaphore.release()
                }
                client.whenStatus().subscribe {
                    when (it) {
                        ComputeClientStatus.CONNECTING -> if (!clientStatusConnecting.compareAndSet(false, true)) Assert.fail("Client: Already been connecting!")
                        ComputeClientStatus.CONNECTED -> {
                            if (!clientStatusConnected.compareAndSet(false, true)) {
                                Assert.fail("Client: Already connected once!")
                            }
                            client.submit(ComputeBatch(1, 100, "HELLO".toByteArray()))
                            client.submit(ComputeError(12345, "bad"))
                            client.submit(ComputeLogEntry(1, 100, "world!"))
                            semaphore.release()
                        }
                        ComputeClientStatus.DISRUPTED -> {
                            Assert.fail("Client: Disrupted unexpectedly.")
                        }
                        ComputeClientStatus.TERMINATED -> if (!clientStatusTerminated.compareAndSet(false, true)) Assert.fail("Client: Already terminated!")
                        else -> Assert.fail("Client: Unexpected status $it.")
                    }
                }
                client.whenException().subscribe {
                    Assert.fail(it.message)
                }

                // Wait for service and client to connect and all messages to be received.
                semaphore.tryAcquire(7, 3, TimeUnit.SECONDS)
            }
            // Wait for service to close automatically in response to client closing.
            semaphore.tryAcquire(1, 2, TimeUnit.SECONDS)
        }

        Assert.assertEquals(1, serviceReceivedBatches.get())
        Assert.assertEquals(1, serviceReceivedErrors.get())
        Assert.assertEquals(1, serviceReceivedLogEntries.get())
        Assert.assertTrue(serviceStatusConnected.get())
        Assert.assertTrue(serviceStatusTerminated.get()) // True because client closed first.

        Assert.assertEquals(1, clientReceivedBatches.get())
        Assert.assertEquals(1, clientReceivedLambdas.get())
        Assert.assertTrue(clientStatusConnecting.get())
        Assert.assertTrue(clientStatusConnected.get())
        Assert.assertFalse(clientStatusTerminated.get()) // False because client closed first.
    }
}