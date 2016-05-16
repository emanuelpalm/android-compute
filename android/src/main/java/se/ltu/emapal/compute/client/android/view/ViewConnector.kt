package se.ltu.emapal.compute.client.android.view

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.LayoutInflater
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.view_connector.view.*
import rx.Observable
import rx.lang.kotlin.PublishSubject
import rx.subjects.PublishSubject
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

    private val onConnectSubject: PublishSubject<String>? = if (isInEditMode) null else PublishSubject()
    private val onDisconnectSubject: PublishSubject<String>? = if (isInEditMode) null else PublishSubject()
    private val onStateChangeSubject: ReplaySubject<State>? = if (isInEditMode) null else ReplaySubject.createWithSize(1)

    /** Fires event containing URI field value whenever the connect button is clicked. */
    val onConnect: Observable<String>
        get() = onConnectSubject ?: Observable.create { }

    /** Fires event containing URI field value whenever the disconnect button is clicked. */
    val onDisconnect: Observable<String>
        get() = onDisconnectSubject ?: Observable.create { }

    /** Fires current state and any subsequent [ViewConnector.State] changes. */
    val onStateChange: Observable<State>
        get() = onStateChangeSubject ?: Observable.create { }

    init {
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE).let {
            it as LayoutInflater
            it.inflate(R.layout.view_connector, this, true)
        }
        field_uri.setOnKeyListener { view, code, event ->
            if (event.action == KeyEvent.ACTION_DOWN && code == KeyEvent.KEYCODE_ENTER) {
                triggerAction()
                true
            } else {
                false
            }
        }
        button_connect.setOnClickListener { triggerAction() }

        onStateChangeSubject?.onNext(State.SHOW_CONNECT)
        onStateChange
                .subscribe { state ->
                    when (state) {
                        State.SHOW_CONNECT -> {
                            text_connect_title.visibility = VISIBLE
                            text_connect_description.visibility = VISIBLE
                            field_uri.isEnabled = true
                            button_connect.isEnabled = true
                            button_connect.text = context.getText(R.string.action_connect)
                        }
                        State.SHOW_CONNECTING -> {
                            field_uri.isEnabled = false
                            button_connect.isEnabled = false
                        }
                        State.SHOW_DISCONNECT -> {
                            text_connect_title.visibility = GONE
                            text_connect_description.visibility = GONE
                            field_uri.isEnabled = false
                            button_connect.isEnabled = true
                            button_connect.text = context.getText(R.string.action_disconnect)
                        }
                        else -> throw IllegalStateException("Bad ViewConnector state: $state.")
                    }
                }
    }

    private fun triggerAction() {
        when (onStateChangeSubject?.value) {
            State.SHOW_CONNECT -> onConnectSubject?.onNext(field_uri.text.toString())
            State.SHOW_DISCONNECT -> onDisconnectSubject?.onNext(field_uri.text.toString())
            else -> throw IllegalStateException("Action triggered while in ${onStateChangeSubject?.value} state.")
        }
    }

    fun setState(state: State) {
        onStateChangeSubject?.onNext(state)
    }

    /**
     * [ViewConnector] state.
     */
    enum class State {
        SHOW_CONNECT,
        SHOW_CONNECTING,
        SHOW_DISCONNECT
    }
}