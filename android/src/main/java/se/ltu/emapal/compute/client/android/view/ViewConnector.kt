package se.ltu.emapal.compute.client.android.view

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.view_connector.view.*
import rx.Observable
import rx.lang.kotlin.PublishSubject
import rx.subjects.PublishSubject
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

    /** Fires event containing URI field value whenever the connect button is clicked. */
    val onConnect: Observable<String>
        get() = onConnectSubject ?: Observable.create { }

    init {
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE).let {
            it as LayoutInflater
            it.inflate(R.layout.view_connector, this, true)
        }
        text_uri.setOnKeyListener { view, code, event ->
            if (event.action == KeyEvent.ACTION_DOWN && code == KeyEvent.KEYCODE_ENTER) {
                onConnectSubject?.onNext((view as EditText).text.toString())
                true
            } else {
                false
            }
        }
        button_connect.setOnClickListener {
            onConnectSubject?.onNext(text_uri.text.toString())
        }
    }
}