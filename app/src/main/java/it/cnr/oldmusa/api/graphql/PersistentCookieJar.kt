package it.cnr.oldmusa.api.graphql

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import java.io.File
import kotlin.collections.HashMap
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableMap
import kotlin.collections.emptyList
import kotlin.collections.map
import kotlin.collections.set
import kotlin.collections.toMap

class PersistentCookieJar(val file: File) : CookieJar {
    private var cache: MutableMap<String, List<Cookie>>? = null
    private val json = Json(JsonConfiguration.Stable.copy(encodeDefaults = false))

    fun requireCache(): MutableMap<String, List<Cookie>> {
        cache?.let {
            return it
        }
        val loaded = loadPersistentFile()
        cache = loaded
        return loaded
    }

    private fun loadPersistentFile(): MutableMap<String, List<Cookie>> {
        if (!file.exists()) return HashMap()

        val data = try {
            json.parse(CookieStorage.serializer(), file.readText(Charsets.UTF_8))
        } catch (e: Exception) {
            return HashMap()
        }

        return data.cookies.map { entry ->
            Pair (
                entry.key,
                entry.value.map { it.toCookie() }
            )
        }.toMap(HashMap())
    }

    private fun savePersistentFile() {
        val toWrite = cache!!.entries.map {
            Pair (
                it.key,
                it.value.map { cookie -> cookie.toSerializable() }
            )
        }.toMap()


        file.writeText(
            json.stringify(CookieStorage.serializer(), CookieStorage(toWrite)),
            Charsets.UTF_8
        )
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        return requireCache()[url.host] ?: emptyList()
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val cache = requireCache()
        if (cache[url.host] == cookies) return
        cache[url.host] = cookies
        savePersistentFile()
    }

    @Serializable
    private data class CookieStorage(
        val cookies: Map<String, List<SerializableCookie>>
    )

    @Serializable
    private data class SerializableCookie(
        val domain: String,
        val name: String,
        val value: String
    ) {
        fun toCookie(): Cookie {
            return Cookie.Builder()
                .domain(domain)
                .name(name)
                .value(value)
                .build()
        }
    }

    private fun Cookie.toSerializable(): SerializableCookie {
        return SerializableCookie(domain, name, value)
    }
}