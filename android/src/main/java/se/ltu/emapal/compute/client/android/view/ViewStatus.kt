package se.ltu.emapal.compute.client.android.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.view_status.view.*
import rx.lang.kotlin.PublishSubject
import se.ltu.emapal.compute.client.android.R

class ViewStatus : LinearLayout {
    /** Context constructor. */
    constructor(context: Context) : super(context)

    /** Context/attribute constructor. */
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    /** Context/attribute/style constructor. */
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE).let {
            it as LayoutInflater
            it.inflate(R.layout.view_status, this, true)
        }
    }

    var lambdaCount: Int
        get() = text_status_lambda_count.text.toString().toInt()
        set(value) {
            text_status_lambda_count.text = value.toString()
        }

    var pendingBatchCount: Int
        get() = text_status_pending_batch_count.text.toString().toInt()
        set(value) {
            text_status_pending_batch_count.text = value.toString()
        }

    var processedBatchCount: Int
        get() = text_status_processed_batch_count.text.toString().toInt()
        set(value) {
            text_status_processed_batch_count.text = value.toString()
        }
}
