package se.ltu.emapal.compute.client.android

import android.os.AsyncTask
import se.ltu.emapal.compute.AndroidComputeContext
import se.ltu.emapal.compute.Computer
import se.ltu.emapal.compute.io.ComputeClientTcp
import se.ltu.emapal.compute.util.Result
import se.ltu.emapal.compute.util.time.Duration
import java.net.InetSocketAddress

/**
 * Creates new [Computer] connected to host identified by provided host name and port.
 *
 * @param onResult Callback invoked after creation and connection either succeeded or failed.
 */
class AsyncTaskCreateComputer(
        private val onResult: (Result<Computer, Throwable>) -> Unit
) : AsyncTask<String, Void?, Result<Computer, Throwable>>() {
    override fun doInBackground(vararg params: String?): Result<Computer, Throwable> {
        try {
            if (params.size != 2) {
                throw IllegalArgumentException("No service address/port provided.")
            }
            val address = params[0]!!
            val port = params[1]!!.toInt()
            val inetSocketAddress = InetSocketAddress(address, port)

            val computeClient = ComputeClientTcp(inetSocketAddress)
            computeClient.awaitConnectionFor(Duration.ofSeconds(15))

            val computeThreads = Runtime.getRuntime().availableProcessors()
            val computer = Computer(computeClient, { AndroidComputeContext() }, computeThreads)
            return Result.Success(computer)

        } catch (e: Throwable) {
            return Result.Failure(e)
        }
    }

    override fun onPostExecute(result: Result<Computer, Throwable>) {
        onResult.invoke(result)
    }
}