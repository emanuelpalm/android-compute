package se.ltu.emapal.compute.client.android

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.Snackbar
import kotlinx.android.synthetic.main.activity_main.*

class ActivityMain : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        view_connector.onConnect.subscribe { uri ->
            Snackbar.make(view_connector, uri, Snackbar.LENGTH_LONG).show()
        }
    }
}
