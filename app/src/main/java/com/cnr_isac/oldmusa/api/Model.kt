package com.cnr_isac.oldmusa.api

import kotlinx.serialization.Optional
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

// Those are the data definitions for the JSON client-server connection
// Note that id and museumId are optional because the client should omit them
// in some cases (for ex. in an update call).
// Fields are renamed from camelCase to sneak_case trough the SerialName annotation

@Serializable
data class ApiUser(
    @Optional val id: Long? = null,
    @Optional val username: String? = null,
    @Optional val password: String? = null,
    @Optional val permission: Char? = null
)

@Serializable
data class ApiId(
        val id: Long
)

@Serializable
data class ApiMuseum(
        @Optional val id: Long? = null,
        @Optional val name: String? = null
)

@Serializable
data class ApiMap(
        @Optional val id: Long? = null,
        @Optional @SerialName("museum_id") val museumId: Long? = null
)


@Serializable
data class ApiRoom(
        @Optional val id: Long? = null,
        @Optional @SerialName("museum_id") val museumId: Long? = null,
        @Optional val name: String? = null
)

@Serializable
data class ApiSensor(
        @Optional val id: Long? = null,
        @Optional @SerialName("museum_id") val museumId: Long? = null,
        @Optional val name: String? = null,
        @Optional val room: Long? = null,
        @Optional @SerialName("range_min") val rangeMin: Long? = null,
        @Optional @SerialName("range_max") val rangeMax: Long? = null,
        @Optional @SerialName("loc_map") val locMap: Long? = null,
        @Optional @SerialName("loc_x") val locX: Long? = null,
        @Optional @SerialName("loc_y") val locY: Long? = null,
        @Optional val enabled: Boolean = false,
        @Optional val status: String = "ok"
)


