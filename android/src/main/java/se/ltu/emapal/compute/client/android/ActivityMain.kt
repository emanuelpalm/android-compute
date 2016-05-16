package se.ltu.emapal.compute.client.android

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import se.ltu.emapal.compute.client.android.view.ViewConnector

class ActivityMain : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        view_connector.listener?.let {

            it.onConnect.subscribe { uri ->
                Snackbar.make(
                        view_connector,
                        applicationContext.getString(R.string.text_connected_to_s, uri),
                        Snackbar.LENGTH_LONG
                ).show()
                view_connector.setState(ViewConnector.State.SHOW_DISCONNECT)
            }

            it.onDisconnect.subscribe { uri ->
                Snackbar.make(
                        view_connector,
                        applicationContext.getString(R.string.text_disconnected_from_s, uri),
                        Snackbar.LENGTH_LONG
                ).show()
                view_connector.setState(ViewConnector.State.SHOW_CONNECT)
            }

        }
    }
}
