package com.cnr_isac.oldmusa.util

import android.app.Activity
import android.content.Context
import android.os.AsyncTask
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import com.cnr_isac.oldmusa.api.RestException
import android.widget.RelativeLayout
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.cnr_isac.oldmusa.Account
import com.cnr_isac.oldmusa.api.Api


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

        override fun onCancelled() {
            query.completeTask(null, null)
        }
    }

    class RawQuery<R>(autoexec: Boolean = true, val f: () -> R) {
        private var resultCallbacks: MutableList<(R) -> Unit> = ArrayList()
        private var errorCallbacks: MutableList<(RestException) -> Unit> = ArrayList()
        private var doneCallbacks: MutableList<() -> Unit> = ArrayList()
        private var task: QueryAsyncTask<R> = QueryAsyncTask(this)

        private var done = false
        private var cancelled = false
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

        fun cancel(mayInterruptIfRunning: Boolean) {
            if (done) return
            this.cancelled = true
            this.task.cancel(mayInterruptIfRunning)
        }

        /**
         * Runs the task, only necessary if autoexec parameter was set to false
         */
        fun execute() {
            task.execute()
        }
    }

    class QueryLifecycleObserver<T>(val query: RawQuery<T>, val mayInterruptIfRunning: Boolean) : LifecycleObserver {
        @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        fun cancelQuery() {
            query.cancel(mayInterruptIfRunning)
        }
    }

    fun <R> Context.query(f: () -> R): RawQuery<R> {
        return RawQuery(true, f).onRestError { handleRestError(this.applicationContext, it) }
    }

    fun <R> Fragment.query(mayInterruptIfRunning: Boolean = false, f: () -> R): RawQuery<R> {
        val query = RawQuery(true, f)

        // Check if the fragment is paused, then cancel it to mitigate exceptions
        val observer = QueryLifecycleObserver(query, mayInterruptIfRunning)
        this.lifecycle.addObserver(observer)
        query.onDone {
            lifecycle.removeObserver(observer)
        }

        query.onRestError { handleRestError(this.context!!.applicationContext, it) }
        return query
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

    /**
     * Displays a loading bar while the query is running.
     * This also disables user interaction
     */
    fun <P> RawQuery<P>.useLoadingBar(context: Activity): RawQuery<P> {
        val layout = RelativeLayout(context)

        val progressBar = ProgressBar(context, null, android.R.attr.progressBarStyleLarge)
        val params = RelativeLayout.LayoutParams(400, 400)
        params.addRule(RelativeLayout.CENTER_IN_PARENT)

        layout.addView(progressBar, params)
        context.addContentView(layout, RelativeLayout.LayoutParams(-1, -1))
        progressBar.visibility = View.VISIBLE  // Show ProgressBar
        context.window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)


        this.onDone {
            progressBar.visibility = View.GONE // Hide ProgressBar
            (layout.parent as ViewGroup).removeView(layout)
            context.window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        }
        return this
    }

    fun <P> RawQuery<P>.useLoadingBar(context: Fragment): RawQuery<P> {
        return useLoadingBar(context.activity!!)
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
