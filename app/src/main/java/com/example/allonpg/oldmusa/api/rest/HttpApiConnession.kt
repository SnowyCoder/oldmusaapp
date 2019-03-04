package com.example.allonpg.oldmusa.api.rest

import java.io.InputStream
import okhttp3.*
import okhttp3.internal.http.HttpMethod.requiresRequestBody
import java.lang.RuntimeException


/**
 * Connection where the REST calls get delivered trough an HTTP/HTTPS channel
 */
class HttpApiConnession(val apiUrl: String) : ApiConnession {
    val client = OkHttpClient()

    private fun rawRequest(
            method: String,
            path: String,
            content: RequestBody?,
            parameters: Map<String, String>?
    ): Response {
        val fullUrl = apiUrl + path
        val req = Request.Builder().run {
            url(fullUrl)
            val corrContent = if (content == null && requiresRequestBody(method)) EMPTY_BODY else content
            method(method, corrContent)
            if (parameters != null) {
                headers(Headers.of(parameters))
            }
            build()
        }
        val res = client.newCall(req).execute()
        if (!res.isSuccessful) {
            val code = res.code()
            val message = res.message()
            throw RuntimeException("$code: '$message' on '$method' '$fullUrl'")
        }
        return res
    }

    override fun connect(
            method: String,
            path: String,
            content: InputStream?,
            contentType: String,
            parameters: Map<String, String>?
    ): InputStream {
        return rawRequest(
                method,
                path,
                content?.let { RequestBody.create(MediaType.get(contentType), it.readBytes()) },
                parameters
        ).use { it.body()!!.byteStream().readBytes().inputStream() }
    }

    override fun connectRest(
            method: String,
            path: String,
            content: String?,
            parameters: Map<String, String>?
    ): String {
        return rawRequest(
                method,
                path,
                content?.let { RequestBody.create(JSON, it) },
                parameters
        ).use { it.body()!!.string() }
    }

    companion object {
        val JSON = MediaType.get("application/json; charset=utf-8")
        val EMPTY_BODY = RequestBody.create(null, "")
    }
}