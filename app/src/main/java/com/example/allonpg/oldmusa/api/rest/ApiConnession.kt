package com.example.allonpg.oldmusa.api.rest

import java.io.InputStream

/**
 * A connection that will deliver the REST calls.
 * REST should always be delivered trough HTTP (that's the whole point) BUT
 * when you are going to make a lot of requests to the server you can also deliver
 * the REST calls over websockets, making the whole thing lighter
 */
interface ApiConnession {
    fun connect(
            method: String,
            path: String,
            content: InputStream? = null,
            contentType: String = "application/json",
            parameters: Map<String, String>? = null
    ): InputStream

    fun connectRest(
            method: String,
            path: String,
            content: String? = null,
            parameters: Map<String, String>? = null
    ): String
}
