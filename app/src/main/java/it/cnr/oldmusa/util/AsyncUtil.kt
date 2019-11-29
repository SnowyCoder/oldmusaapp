package it.cnr.oldmusa.util

import android.content.Context
import android.os.AsyncTask
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent


object AsyncUtil {
    class CustomAsyncTask<R>(val query: RawTask<R>) : AsyncTask<Void, Void, Void>() {
        var result: R? = null
        var error: Exception? = null

        override fun doInBackground(vararg params: Void?): Void? {
            try {
                result = query.f()
            } catch (e: Exception) {
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

    class RawTask<R>(autoexec: Boolean = true, val f: () -> R) {
        private var resultCallbacks: MutableList<(R) -> Unit> = ArrayList()
        private var errorCallbacks: MutableList<(Exception) -> Unit> = ArrayList()
        private var doneCallbacks: MutableList<() -> Unit> = ArrayList()
        private var task: CustomAsyncTask<R> = CustomAsyncTask(this)

        private var done = false
        private var cancelled = false
        private var result: R? = null
        private var error: Exception? = null

        init {
            if (autoexec) task.execute()
        }

        fun onResult(callback: (R) -> Unit): RawTask<R> {
            when (done) {
                false -> resultCallbacks.add(callback)
                true -> result?.let(callback)
            }
            return this
        }

        fun onError(callback: (Exception) -> Unit): RawTask<R> {
            when (done) {
                false -> errorCallbacks.add(callback)
                true -> error?.let(callback)
            }
            return this
        }

        fun onDone(callback: () -> Unit): RawTask<R> {
            when (done) {
                false -> doneCallbacks.add(callback)
                true -> callback()
            }
            return this
        }

        fun completeTask(result: R?, error: Exception?) {
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

    class QueryLifecycleObserver<T>(val query: RawTask<T>, val mayInterruptIfRunning: Boolean) : LifecycleObserver {
        @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        fun cancelQuery() {
            query.cancel(mayInterruptIfRunning)
        }
    }

    fun <R> Context.async(f: () -> R): RawTask<R> {
        return RawTask(true, f).onError { handleError(this.applicationContext, it) }
    }

    fun <R> Fragment.async(mayInterruptIfRunning: Boolean = false, defaultErrorHandler: Boolean = true, f: () -> R): RawTask<R> {
        val query = RawTask(true, f)

        // Check if the fragment is paused, then cancel it to mitigate exceptions
        val observer = QueryLifecycleObserver(query, mayInterruptIfRunning)
        this.lifecycle.addObserver(observer)
        query.onDone {
            lifecycle.removeObserver(observer)
        }

        if (defaultErrorHandler) {
            query.onError { handleError(this.context!!.applicationContext, it) }
        }
        return query
    }

    fun handleError(ctx: Context, e: Exception) {
        Log.e("Rest", "An exception occured", e)
    }

    fun isMainThread(): Boolean = Looper.myLooper() == Looper.getMainLooper()
}
