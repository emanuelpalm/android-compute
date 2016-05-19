package se.ltu.emapal.compute.client.android.view

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.LayoutInflater
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.view_connector.view.*
import rx.Observable
import rx.lang.kotlin.PublishSubject
import rx.subjects.ReplaySubject
import se.ltu.emapal.compute.client.android.R

/**
 * Code-behind for [R.layout.view_connector].
 */
class ViewConnector : LinearLayout {
    /** Context constructor. */
    constructor(context: Context) : super(context)

    /** Context/attribute constructor. */
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    /** Context/attribute/style constructor. */
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    /** Exposes [ViewConnector] events. */
    val listener: Listener? = if (!isInEditMode) Listener() else null

    init {
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE).let {
            it as LayoutInflater
            it.inflate(R.layout.view_connector, this, true)
        }
        field_port.setOnKeyListener { view, code, event ->
            if (event.action == KeyEvent.ACTION_DOWN && code == KeyEvent.KEYCODE_ENTER) {
                triggerAction()
                true
            } else {
                false
            }
        }
        button_connect.setOnClickListener { triggerAction() }

        listener?.onStateChangeSubject?.let {
            it.onNext(State.SHOW_CONNECT)
            it.subscribe { state ->
                when (state) {
                    State.SHOW_CONNECT -> {
                        text_connect_title.visibility = VISIBLE
                        text_connect_description.visibility = VISIBLE
                        field_address.isEnabled = true
                        button_connect.isEnabled = true
                        button_connect.text = context.getText(R.string.action_connect)
                    }
                    State.SHOW_CONNECTING -> {
                        field_address.isEnabled = false
                        button_connect.isEnabled = false
                    }
                    State.SHOW_DISCONNECT -> {
                        text_connect_title.visibility = GONE
                        text_connect_description.visibility = GONE
                        field_address.isEnabled = false
                        button_connect.isEnabled = true
                        button_connect.text = context.getText(R.string.action_disconnect)
                    }
                    else -> throw IllegalStateException("Bad ViewConnector state: $state.")
                }
            }
        }
    }

    private fun triggerAction() {
        listener?.let {
            val state = it.onStateChangeSubject.value
            val pair = Pair(field_address.text.toString(), field_port.text.toString())
            when (state) {
                State.SHOW_CONNECT -> it.onConnectSubject.onNext(pair)
                State.SHOW_DISCONNECT -> it.onDisconnectSubject.onNext(pair)
                else -> throw IllegalStateException("Action triggered while in $state state.")
            }
        }
    }

    /** Sets view state. */
    fun setState(state: State) {
        listener?.onStateChangeSubject?.onNext(state)
    }

    /**
     * [ViewConnector] state.
     */
    enum class State {
        SHOW_CONNECT,
        SHOW_CONNECTING,
        SHOW_DISCONNECT
    }

    class Listener {
        internal val onConnectSubject = PublishSubject<Pair<String, String>>()
        internal val onDisconnectSubject = PublishSubject<Pair<String, String>>()
        internal val onStateChangeSubject = ReplaySubject.createWithSize<State>(1)

        /** Fires event containing URI field value whenever the connect button is clicked. */
        val whenConnect: Observable<Pair<String, String>>
            get() = onConnectSubject

        /** Fires event containing URI field value whenever the disconnect button is clicked. */
        val whenDisconnect: Observable<Pair<String, String>>
            get() = onDisconnectSubject
    }
}