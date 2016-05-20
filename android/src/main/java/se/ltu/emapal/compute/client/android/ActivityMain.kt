package se.ltu.emapal.compute.client.android

import android.os.AsyncTask
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*
import rx.android.schedulers.AndroidSchedulers
import se.ltu.emapal.compute.Computer
import se.ltu.emapal.compute.client.android.view.ViewConnector
import se.ltu.emapal.compute.io.ComputeClientStatus
import se.ltu.emapal.compute.util.Result
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.util.concurrent.atomic.AtomicReference

class ActivityMain : AppCompatActivity() {
    val computer = AtomicReference<Computer?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        view_connector.listener?.let {

            it.whenConnect.subscribe { addressPair ->
                val address = addressPair.first
                val port = addressPair.second

                view_connector.setState(ViewConnector.State.SHOW_CONNECTING)
                startComputer(address, port)
            }

            it.whenDisconnect.subscribe { addressPair ->
                showSnackbar(R.string.text_disconnected_from_ss, addressPair.first, addressPair.second)
                view_connector.setState(ViewConnector.State.SHOW_CONNECT)
                view_status.visibility = View.GONE
                stopComputer()
            }

        }
    }

    private fun showSnackbar(resourceId: Int, vararg parameters: Any) {
        Snackbar.make(view_connector, getString(resourceId, *parameters), Snackbar.LENGTH_LONG)
                .show()
    }

    private fun startComputer(address: String, port: String) {
        AsyncTaskCreateComputer({ result ->
            when (result) {
                is Result.Success -> {
                    showSnackbar(R.string.text_connected_to_ss, address, port)

                    computer.set(result.value)
                    val computer = result.value

                    computer.whenLambdaCount
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe { view_status.lambdaCount = it }

                    computer.whenBatchPendingCount
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe { view_status.pendingBatchCount = it }

                    computer.whenBatchProcessedCount
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe { view_status.processedBatchCount = it }

                    computer.whenException
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe {
                                Log.e(javaClass.simpleName, "Compute context ${it.first} error.", it.second)
                                stopComputer()
                            }

                    computer.client.whenException
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe {
                                Log.e(javaClass.simpleName, "Compute client error.", it)
                                stopComputer()
                            }

                    computer.client.whenStatus
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe {
                                when (it) {
                                    ComputeClientStatus.DISRUPTED -> {
                                        showSnackbar(R.string.text_connection_lost_to_ss, address, port)
                                        Log.w(javaClass.simpleName, "Compute client disrupted.")
                                        view_status.visibility = View.GONE
                                        view_connector.setState(ViewConnector.State.SHOW_CONNECT)
                                    }
                                    ComputeClientStatus.TERMINATED -> {
                                        showSnackbar(R.string.text_connection_closed_by_ss, address, port)
                                        view_status.visibility = View.GONE
                                        view_connector.setState(ViewConnector.State.SHOW_CONNECT)
                                    }
                                    else -> Unit
                                }
                            }

                    view_connector.setState(ViewConnector.State.SHOW_DISCONNECT)

                    view_status.alpha = 0.0f
                    view_status.visibility = View.VISIBLE
                    view_status.animate()
                            .alpha(1.0f)
                            .setStartDelay(800)
                            .setDuration(156)
                            .start()
                }
                is Result.Failure -> {
                    val resource = when (result.error) {
                        is IllegalArgumentException -> R.string.text_error_address
                        is ConnectException -> R.string.text_error_failed_to_connect_to_ss
                        is SocketTimeoutException -> R.string.text_error_timed_out_connecting_to_ss
                        is SocketException -> {
                            Log.w(javaClass.simpleName, "Failed to connect to service.", result.error)
                            R.string.text_error_failed_to_connect_to_ss
                        }
                        else -> {
                            Log.e(javaClass.simpleName, "Failed to connect to service.", result.error)
                            R.string.text_error_failed_to_connect_to_ss
                        }
                    }
                    showSnackbar(resource, address, port)

                    view_connector.setState(ViewConnector.State.SHOW_CONNECT)
                }
            }
        }).execute(address, port)
    }

    fun stopComputer() {
        AsyncTask.execute {
            computer.get()?.let {
                it.close()
                it.client.close()
            }
        }
    }
}
