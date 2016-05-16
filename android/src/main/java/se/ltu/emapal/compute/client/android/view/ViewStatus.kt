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

    /** Allows getting and settings [ViewStatus] properties. */
    val subject = if (!isInEditMode) Subject() else null

    init {
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE).let {
            it as LayoutInflater
            it.inflate(R.layout.view_status, this, true)
        }

        subject?.let {
            it.whenLambdaCount.subscribe {
                text_status_lambda_count.text = it.toString()
            }
            it.whenPendingBatchCount.subscribe {
                text_status_pending_batch_count.text = it.toString()
            }
            it.whenProcessedBatchCount.subscribe {
                text_status_processed_batch_count.text = it.toString()
            }
        }
    }

    class Subject {
        /** Registered lambda count. */
        val whenLambdaCount = PublishSubject<Int>()

        /** Pending batch count. */
        val whenPendingBatchCount = PublishSubject<Int>()

        /** Processed batch count. */
        val whenProcessedBatchCount = PublishSubject<Int>()
    }
}
