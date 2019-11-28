package it.cnr.oldmusa

import android.content.Context
import com.apollographql.apollo.ApolloClient
import it.cnr.oldmusa.api.graphql.NaiveDateTimeConverter
import it.cnr.oldmusa.api.graphql.PersistentCookieJar
import it.cnr.oldmusa.type.CustomType
import okhttp3.OkHttpClient
import java.io.File
import java.nio.charset.StandardCharsets

object Account {
    private var lhttpClient: OkHttpClient? = null
    private var lapollo: ApolloClient? = null
    const val DEFAULT_URL = "http://192.168.1.111:8080/api/"// Server URL
    private const val TAG = "Account"

    private var _isAdmin: Boolean = false

    val isAdmin: Boolean
        get() = _isAdmin


    fun resetAdminCache(isAdmin: Boolean) {
        this._isAdmin = isAdmin
    }

    fun getUrl(context: Context): String {
        val file = File(context.filesDir, "url")

        if (!file.exists()) return DEFAULT_URL

        return file.readBytes().toString(Charsets.UTF_8).ifBlank { DEFAULT_URL }
    }

    fun setUrl(context: Context, string: String) {
        lapollo = null
        File(context.filesDir, "url").writeText(string, StandardCharsets.UTF_8)
    }

    private fun loadHttpClient(context: Context): OkHttpClient {
        return OkHttpClient
            .Builder()
            .cookieJar(PersistentCookieJar(File(context.filesDir, "cookies")))
            .build()
    }

    private fun loadApollo(context: Context): ApolloClient {
        val okHttp = getHttpClient(context)

        return ApolloClient.builder()
            .serverUrl(getUrl(context) + "graphql")
            .okHttpClient(okHttp)
            .addCustomTypeAdapter(CustomType.NAIVEDATETIME, NaiveDateTimeConverter())
            .build()
    }

    @Synchronized
    fun getHttpClient(context: Context): OkHttpClient {
        lhttpClient?.let { return it }
        val httpClient = loadHttpClient(context)
        lhttpClient = httpClient

        return httpClient
    }

    @Synchronized
    fun getApollo(context: Context): ApolloClient {
        lapollo?.let { return it }
        val apollo = loadApollo(context)
        lapollo = apollo

        return apollo
    }
}