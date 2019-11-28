package it.cnr.oldmusa.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Mutation
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.Query
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloCanceledException
import com.apollographql.apollo.exception.ApolloException
import it.cnr.oldmusa.Account
import it.cnr.oldmusa.api.graphql.ServiceGraphQlException
import it.cnr.oldmusa.api.graphql.UnauthorizedraphQlException
import it.cnr.oldmusa.type.PermissionType
import it.cnr.oldmusa.util.AsyncUtil.async
import it.cnr.oldmusa.util.AsyncUtil.isMainThread
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.InputStream


object GraphQlUtil {
    class QueryCallback<R>(val query: RawCall<R>) : ApolloCall.Callback<R>() {
        override fun onResponse(response: Response<R>) {
            if (response.hasErrors()) {
                val error = ServiceGraphQlException.fromErrors(query.call.operation().name().name(), response.errors())
                query.completeTask(null, error)
            } else {
                query.completeTask(response.data(), null)
            }
        }

        override fun onFailure(e: ApolloException) {
            query.completeTask(null, e)
        }


        override fun onCanceledError(e: ApolloCanceledException) {
            query.completeTask(null, null)
        }
    }

    class RawCall<R>(autoexec: Boolean = true, val call: ApolloCall<R>) {
        private var resultCallbacks: MutableList<(R) -> Unit> = ArrayList()
        private var errorCallbacks: MutableList<(Exception) -> Unit> = ArrayList()
        private var doneCallbacks: MutableList<() -> Unit> = ArrayList()
        private var callback: QueryCallback<R> = QueryCallback(this)

        private var done = false
        private var cancelled = false
        private var result: R? = null
        private var error: Exception? = null

        init {
            if (autoexec) execute()
        }

        fun onResult(callback: (R) -> Unit): RawCall<R> {
            when (done) {
                false -> resultCallbacks.add(callback)
                true -> result?.let(callback)
            }
            return this
        }

        fun onServiceError(callback: (ServiceGraphQlException) -> Unit): RawCall<R> {
            return onError {
                if (it is ServiceGraphQlException) {
                    callback(it)
                }
            }
        }

        fun onError(callback: (Exception) -> Unit): RawCall<R> {
            when (done) {
                false -> errorCallbacks.add(callback)
                true -> error?.let(callback)
            }
            return this
        }

        fun onDone(callback: () -> Unit): RawCall<R> {
            when (done) {
                false -> doneCallbacks.add(callback)
                true -> callback()
            }
            return this
        }

        fun completeTask(result: R?, error: Exception?) {
            assert(!done) { "Task already completed" }

            if (!isMainThread()) {
                Handler(Looper.getMainLooper()).post { completeTask(result, error) }
                return
            }

            this.result = result
            this.error = error
            done = true

            result?.let {res -> resultCallbacks.forEach { cb -> cb(res) } }
            error?.let { err -> errorCallbacks.forEach { cb -> cb(err) } }
            doneCallbacks.forEach { it() }
        }

        fun cancel() {
            if (done) return
            this.cancelled = true
            this.call.cancel()
        }

        /**
         * Runs the task, only necessary if autoexec parameter was set to false
         */
        fun execute() {
            call.enqueue(callback)
        }
    }

    class QueryLifecycleObserver<T>(val query: RawCall<T>) : LifecycleObserver {
        @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        fun cancelQuery() {
            query.cancel()
        }
    }

    fun <D : Operation.Data, T, V : Operation.Variables> Fragment.query(q: Query<D, T, V>): RawCall<T> {
        return apolloCall(this.apollo.query(q))
    }

    fun <D : Operation.Data, T, V : Operation.Variables> Fragment.mutate(q: Mutation<D, T, V>): RawCall<T> {
        return apolloCall(this.apollo.mutate(q))
    }

    fun <T> Fragment.apolloCall(call: ApolloCall<T>, manageError: Boolean = true): RawCall<T> {
        val rawCall = RawCall(true, call)

        val observer = QueryLifecycleObserver(rawCall)
        this.lifecycle.addObserver(observer)
        rawCall.onDone {
            lifecycle.removeObserver(observer)
        }

        if (manageError) {
            rawCall.onError { handleError(this.requireContext().applicationContext, it) }
        }
        return rawCall
    }

    fun <T> Context.apolloCall(call: ApolloCall<T>, manageError: Boolean = true): RawCall<T> {
        val rawCall = RawCall(true, call)

        if (manageError) {
            rawCall.onError { handleError(this.applicationContext, it) }
        }
        return rawCall
    }

    fun handleError(ctx: Context, e: Exception) {
        when (e) {
            is UnauthorizedraphQlException -> {
                // LoginFragment error, token invalid or outdated
                Toast.makeText(ctx, "LoginFragment error", Toast.LENGTH_LONG).show()
            }
            else -> {
                Toast.makeText(ctx, "${e.message}", Toast.LENGTH_LONG).show()
            }
        }
        Log.e("GraphQl", "An exception occured", e)
    }

    val Fragment.apollo: ApolloClient
        inline get() = Account.getApollo(this.context!!.applicationContext)

    val Context.apollo: ApolloClient
        inline get() = Account.getApollo(applicationContext)

    fun PermissionType.asChar(): Char {
        return when (this) {
            PermissionType.ADMIN -> 'A'
            PermissionType.USER -> 'U'
            else -> throw RuntimeException("Illegal permission")
        }
    }

    fun Char.toPermissionType(): PermissionType {
        return when (this) {
            'A' -> PermissionType.ADMIN
            'U' -> PermissionType.USER
            else -> throw RuntimeException("Illegal permission")
        }
    }

    fun downloadImage(context: Context, siteId: Int): AsyncUtil.RawTask<Bitmap?> {
        return context.async {
            val httpClient = Account.getHttpClient(context)

            val fullUrl = Account.getUrl(context) + "site_map/" + siteId

            val req = Request.Builder().run {
                url(fullUrl)
                get()
                build()
            }

            val res = httpClient.newCall(req).execute()
            if (!res.isSuccessful) {
                if (res.code == 404) {
                    return@async null
                }
                throw ServiceGraphQlException.fromHttpCode("SiteImage", res.code, res.body?.toString())
            }

            res.body?.byteStream()?.readBytes()?.let {
                BitmapFactory.decodeByteArray(it, 0, it.size)
            }
        }
    }

    fun uploadImage(context: Context, siteId: Int, image: InputStream, resizeData: MapResizeData? = null): AsyncUtil.RawTask<Bitmap?> {
        return context.async {
            val httpClient = Account.getHttpClient(context)

            var fullUrl = Account.getUrl(context) + "site_map/" + siteId

            if (resizeData != null) {
                fullUrl += "?fromH=" + resizeData.fromH + "&fromW=" + resizeData.fromW +
                        "&toH=" + resizeData.toH + "&toW=" + resizeData.toW
            }

            val req = Request.Builder().run {
                url(fullUrl)
                post(image.readBytes().toRequestBody("image/png".toMediaType()))
                build()
            }

            val res = httpClient.newCall(req).execute()
            if (!res.isSuccessful) {
                if (res.code == 404) {
                    return@async null
                }
                throw ServiceGraphQlException.fromHttpCode("SiteImage", res.code, res.body?.toString())
            }

            res.body?.byteStream()?.readBytes()?.let {
                BitmapFactory.decodeByteArray(it, 0, it.size)
            }
        }
    }

    data class MapResizeData(
        val fromW: Int,
        val fromH: Int,
        val toW: Int,
        val toH: Int
    )
}
