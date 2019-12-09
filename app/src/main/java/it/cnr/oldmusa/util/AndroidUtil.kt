package it.cnr.oldmusa.util

import android.app.Activity
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AutoCompleteTextView
import android.widget.ProgressBar
import android.widget.RelativeLayout
import androidx.fragment.app.Fragment
import it.cnr.oldmusa.util.AndroidUtil.alwaysComplete

object AndroidUtil {
    fun Any.toNullableString(): String? {
        val str = this.toString()
        if (str.isBlank()) return null
        return str
    }

    private fun addLoadingBar(context: Activity): () -> Unit {
        val layout = RelativeLayout(context)

        val progressBar = ProgressBar(context, null, android.R.attr.progressBarStyleLarge)
        val params = RelativeLayout.LayoutParams(400, 400)
        params.addRule(RelativeLayout.CENTER_IN_PARENT)

        layout.addView(progressBar, params)
        context.addContentView(layout, RelativeLayout.LayoutParams(-1, -1))
        progressBar.visibility = View.VISIBLE  // Show ProgressBar
        context.window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)

        return {
            progressBar.visibility = View.GONE // Hide ProgressBar
            (layout.parent as ViewGroup).removeView(layout)
            context.window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        }
    }

    fun AutoCompleteTextView.alwaysComplete() {
        this.threshold = 1
        this.setOnTouchListener { v, event -> if (this.text.isEmpty()) this.showDropDown(); false }
        val textView = this
        this.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (s?.isEmpty() == true) {
                    Handler().postDelayed({
                        //manually show drop down
                        textView.showDropDown()
                    }, 100) // with 100 millis of delay
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
        })
    }

    /**
     * Displays a loading bar while the query is running.
     * This also disables user interaction
     */
    fun <P> AsyncUtil.RawTask<P>.useLoadingBar(context: Activity): AsyncUtil.RawTask<P> {
        this.onDone(addLoadingBar(context))
        return this
    }

    fun <P> AsyncUtil.RawTask<P>.useLoadingBar(context: Fragment): AsyncUtil.RawTask<P> {
        return useLoadingBar(context.activity!!)
    }

    /**
     * Displays a loading bar while the query is running.
     * This also disables user interaction
     */
    fun <P> GraphQlUtil.RawCall<P>.useLoadingBar(context: Activity): GraphQlUtil.RawCall<P> {
        this.onDone(addLoadingBar(context))
        return this
    }

    fun <P> GraphQlUtil.RawCall<P>.useLoadingBar(context: Fragment): GraphQlUtil.RawCall<P> {
        return useLoadingBar(context.activity!!)
    }

}