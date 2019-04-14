package com.cnr_isac.oldmusa

import android.content.Context
import android.content.ContextWrapper
import com.cnr_isac.oldmusa.api.Api
import com.cnr_isac.oldmusa.api.rest.RestApi
import java.io.File
import java.nio.charset.StandardCharsets

object Account {
    private var lapi: Api? = null
    const val DEFAULT_URL = "http://localhost/api/"// Server URL


    private fun loadToken(context: Context): String {
        val file = File(context.filesDir, "token")

        if (!file.exists()) return ""

        return file.readBytes().toString(Charsets.UTF_8)
    }

    fun saveToken(context: Context) {
        val api = lapi ?: return
        val token = api.getCurrentToken() ?: ""

        File(context.filesDir, "token").writeText(token, Charsets.UTF_8)
    }

    fun getUrl(context: Context): String {
        val file = File(context.filesDir, "url")

        if (!file.exists()) return DEFAULT_URL

        return file.readBytes().toString().ifBlank { DEFAULT_URL }
    }

    fun setUrl(context: Context, string: String) {
        lapi = null
        File(context.filesDir, "url").writeText(string, StandardCharsets.UTF_8)
    }

    private fun loadApi(context: Context): Api {
        // TODO: websocket api logic
        return RestApi.httpRest(getUrl(context)).also {
            it.useToken(loadToken(context))
        }
    }

    fun getApi(context: Context): Api {
        lapi?.let { return it }
        val api = loadApi(context)
        lapi = api
        return api
    }

    val ContextWrapper.api: Api
        inline get() = getApi(applicationContext)
}