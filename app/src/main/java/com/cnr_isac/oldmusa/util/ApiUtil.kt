package com.cnr_isac.oldmusa.util

import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.AsyncTask
import android.os.Looper
import androidx.constraintlayout.widget.ConstraintLayout
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.cnr_isac.oldmusa.api.RestException
import android.widget.RelativeLayout
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import com.cnr_isac.oldmusa.Account
import com.cnr_isac.oldmusa.Login
import com.cnr_isac.oldmusa.api.Api
import kotlinx.android.synthetic.main.list_item.view.*


object ApiUtil {
    class QueryAsyncTask<R>(val query: RawQuery<R>) : AsyncTask<Void, Void, Void>() {
        var result: R? = null
        var error: RestException? = null

        override fun doInBackground(vararg params: Void?): Void? {
            try {
                result = query.f()
            } catch (e: RestException) {
                error = e
            }
            return null
        }

        override fun onPostExecute(r: Void?) {
            query.completeTask(result, error)
        }
    }

    class RawQuery<R>(autoexec: Boolean = true, val f: () -> R) {
        private var resultCallbacks: MutableList<(R) -> Unit> = ArrayList()
        private var errorCallbacks: MutableList<(RestException) -> Unit> = ArrayList()
        private var doneCallbacks: MutableList<() -> Unit> = ArrayList()
        private var task: QueryAsyncTask<R> = QueryAsyncTask(this)

        private var done = false
        private var result: R? = null
        private var error: RestException? = null

        init {
            if (autoexec) task.execute()
        }

        fun onResult(callback: (R) -> Unit): RawQuery<R> {
            when (done) {
                false -> resultCallbacks.add(callback)
                true -> result?.let(callback)
            }
            return this
        }

        fun onRestError(callback: (RestException) -> Unit): RawQuery<R> {
            when (done) {
                false -> errorCallbacks.add(callback)
                true -> error?.let(callback)
            }
            return this
        }

        fun onDone(callback: () -> Unit): RawQuery<R> {
            when (done) {
                false -> doneCallbacks.add(callback)
                true -> callback()
            }
            return this
        }

        fun completeTask(result: R?, error: RestException?) {
            assert(!done) { "Task already completed" }
            this.result = result
            this.error = error
            done = true

            result?.let {res -> resultCallbacks.forEach { cb -> cb(res) } }
            error?.let { err -> errorCallbacks.forEach { cb -> cb(err) } }
            doneCallbacks.forEach { it() }
        }

        /**
         * Runs the task, only necessary if autoexec parameter was set to false
         */
        fun execute() {
            task.execute()
        }
    }

    fun <R> Context.query(f: () -> R): RawQuery<R> {
        return RawQuery(true, f).onRestError { handleRestError(this.applicationContext, it) }
    }

    fun <R> Fragment.query(f: () -> R): RawQuery<R> {
        return RawQuery(true, f).onRestError { handleRestError(this.context!!.applicationContext, it) }
    }

    fun handleRestError(ctx: Context, e: RestException) {
        if (e.code == 401 /* Unauthorized */ && e.responseContent != null && e.responseContent.contains("Invalid token")) {
            // Login error, token invalid or outdated
            Toast.makeText(ctx, "Login error", Toast.LENGTH_LONG).show()
        } else {
            // TODO: parse json and show only message
            Toast.makeText(ctx, "${e.code} ${e.responseContent}", Toast.LENGTH_LONG).show()
        }
        Log.e("Rest", "A Rest exception occured", e)
    }

    fun <P> RawQuery<P>.withLoading(context: Activity): RawQuery<P> {
        val layout = RelativeLayout(context)

        val progressBar = ProgressBar(context, null, android.R.attr.progressBarStyleLarge)
        val params = RelativeLayout.LayoutParams(400, 400)
        params.addRule(RelativeLayout.CENTER_IN_PARENT)

        layout.addView(progressBar, params)
        context.addContentView(layout, RelativeLayout.LayoutParams(-1, -1))
        progressBar.visibility = View.VISIBLE  // Show ProgressBar


        this.onDone {
            progressBar.visibility = View.GONE // Hide ProgressBar
            (layout.parent as ViewGroup).removeView(layout)
        }
        return this
    }

    fun <P> RawQuery<P>.withLoading(context: Fragment): RawQuery<P> {
        return withLoading(context.activity!!)
    }

    val Context.api: Api
        inline get() = Account.getApi(applicationContext)

    val Fragment.api: Api
        inline get() = Account.getApi(this.context!!.applicationContext)

    fun Context.isAdmin(f: (Boolean) -> Unit) {
        Account.getApi(this) { Account.isAdmin(it, f) }
    }

    fun Fragment.isAdmin(f: (Boolean) -> Unit) {
        Account.getApi(context!!) { Account.isAdmin(it, f) }
    }

    fun isMainThread(): Boolean = Looper.myLooper() == Looper.getMainLooper()
}
