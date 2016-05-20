package se.ltu.emapal.compute

import se.ltu.emapal.compute.io.ComputeServiceTcpListener
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.exitProcess

/**
 * Application main function.
 */
fun main(args: Array<String>) {
    if (args.size != 1 || !args[0].matches(Regex("^[0-9]{1,5}$"))) {
        panic("Please provide a valid port number (0-65535) as argument.")
    }
    val port = args[0].toInt()
    if (port > 65536) {
        panic("Please provide a valid port number (0-65535) as argument.")
    }

    println("Palm/compute server")
    print("Starting up ... ")

    val listener = ComputeServiceTcpListener(InetSocketAddress(port))
    listener.whenConnect.subscribe { client ->
        val batchCounter = AtomicInteger(0)

        println("$client: Connected.")

        client.whenBatch.subscribe { batch ->
            println("$client: Received batch \"${batch.data.toString(StandardCharsets.UTF_8)}\".")

            client.submit(ComputeBatch(1, batchCounter.andIncrement, "hello ${batchCounter.get()}!".toByteArray(StandardCharsets.UTF_8)))
        }
        client.whenError.subscribe { error ->
            println("$client: Received error [${error.code}] \"${error.message}\".")
        }
        client.whenLogEntry.subscribe { logEntry ->
            println("$client: Received log entry ${logEntry.timestamp} [${logEntry.lambdaId},${logEntry.batchId}] \"${logEntry.message}\".")
        }
        client.whenStatus.subscribe { status ->
            println("$client: Got status [$status].")
        }
        client.whenException.subscribe {
            println("$client: Caught exception.")
            it.printStackTrace()
            client.close()
        }

        client.submit(ComputeLambda(1, "" +
                "lcm:register(function (batch)\n" +
                "  return batch:upper()\n" +
                "end)\n"))

        client.submit(ComputeBatch(1, batchCounter.andIncrement, "hello".toByteArray(StandardCharsets.UTF_8)))
    }
    listener.whenException.subscribe {
        it.printStackTrace()
    }
    Runtime.getRuntime().addShutdownHook(Thread {
        print("Shutting down ... ")
        listener.close()
        println("OK!")
    })

    println("OK!")
}

fun panic(message: String) {
    println(message)
    exitProcess(1)
}