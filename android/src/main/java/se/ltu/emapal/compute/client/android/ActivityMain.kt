package se.ltu.emapal.compute.client.android

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*
import se.ltu.emapal.compute.client.android.view.ViewConnector

class ActivityMain : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        view_connector.listener?.let {

            it.whenConnect.subscribe { addressPair ->
                Snackbar.make(
                        view_connector,
                        applicationContext.getString(R.string.text_connected_to_ss, addressPair.first, addressPair.second),
                        Snackbar.LENGTH_LONG
                ).show()
                view_connector.setState(ViewConnector.State.SHOW_DISCONNECT)

                view_console.alpha = 0.0f
                view_console.visibility = View.VISIBLE
                view_console.animate()
                        .alpha(1.0f)
                        .setStartDelay(800)
                        .setDuration(156)
                        .start()
            }

            it.whenDisconnect.subscribe { addressPair ->
                Snackbar.make(
                        view_connector,
                        applicationContext.getString(R.string.text_disconnected_from_ss, addressPair.first, addressPair.second),
                        Snackbar.LENGTH_LONG
                ).show()
                view_connector.setState(ViewConnector.State.SHOW_CONNECT)

                view_console.visibility = View.GONE
            }

        }
    }
}
