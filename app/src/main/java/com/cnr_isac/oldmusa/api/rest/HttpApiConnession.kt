package com.cnr_isac.oldmusa.api.rest

import com.cnr_isac.oldmusa.api.RestException
import okhttp3.*
import okhttp3.internal.Util
import okhttp3.internal.http.HttpMethod.requiresRequestBody
import java.io.InputStream


/**
 * Connection where the REST calls get delivered trough an HTTP/HTTPS channel
 */
class HttpApiConnession(apiUrl: String) : ApiConnession {
    val client = OkHttpClient()
    private val httpApiUrl = HttpUrl.parse(apiUrl) ?: throw RuntimeException("Invalid URL $apiUrl")


    private fun rawRequest(
            method: String,
            path: String,
            parameters: Map<String, String>?,
            content: RequestBody?,
            headers: Map<String, String>?
    ): Response {
        val fullUrl = httpApiUrl.newBuilder().addPathSegment(path)

        if (parameters != null) {
            for ((key, value) in parameters) {
                fullUrl.addQueryParameter(key, value)
            }
        }

        val req = Request.Builder().run {
            url(fullUrl.build())
            val corrContent = if (content == null && requiresRequestBody(method)) Util.EMPTY_REQUEST else content
            method(method, corrContent)
            if (headers != null) {
                headers(Headers.of(headers))
            }
            build()
        }
        val res = client.newCall(req).execute()
        if (!res.isSuccessful) {
            val code = res.code()
            val message = res.message()
            val verbose = res.body()?.string()
            throw RestException(code, message, method, fullUrl.toString(), verbose)
        }
        return res
    }

    override fun connect(
            method: String,
            path: String,
            parameters: Map<String, String>?,
            content: InputStream?,
            contentType: String,
            headers: Map<String, String>?
    ): InputStream {
        return rawRequest(
            method,
            path,
            parameters,
            content?.let { RequestBody.create(MediaType.get(contentType), it.readBytes()) },
            headers
        ).use { it.body()!!.byteStream().readBytes().inputStream() }
    }

    override fun connectRest(
            method: String,
            path: String,
            parameters: Map<String, String>?,
            content: String?,
            headers: Map<String, String>?
    ): String {
        return rawRequest(
            method,
            path,
            parameters,
            content?.let { RequestBody.create(JSON, it) },
            headers
        ).use { it.body()!!.string() }
    }

    companion object {
        val JSON = MediaType.get("application/json; charset=utf-8")
        // val EMPTY_BODY = RequestBody.create(null, "")
    }
}