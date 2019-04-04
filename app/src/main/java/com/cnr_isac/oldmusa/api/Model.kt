package com.cnr_isac.oldmusa.api

import kotlinx.serialization.Optional
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import java.time.LocalDate

// Those are the data definitions for the JSON client-server connection
// Note that id and siteId are optional because the client should omit them
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
data class ApiSite(
    @Optional val id: Long? = null,
    @Optional @SerialName("id_cnr") val idCnr: String? = null,
    @Optional val name: String? = null
)

@Serializable
data class ApiMap(
    @Optional val id: Long? = null,
    @Optional @SerialName("site_id") val siteId: Long? = null
)


@Serializable
data class ApiChannel(
    @Optional val id: Long? = null,
    @Optional @SerialName("sensor_id") val sensorId: Long? = null,
    @Optional @SerialName("id_cnr") val idCnr: Long? = null,

    @Optional val name: String? = null,

    @Optional @SerialName("measure_unit") val measureUnit: String? = null,
    @Optional @SerialName("range_min") val rangeMin: Double? = null,
    @Optional @SerialName("range_max") val rangeMax: Double? = null
)

@Serializable
data class ApiSensor(
    @Optional val id: Long? = null,
    @Optional @SerialName("site_id") val siteId: Long? = null,
    @Optional @SerialName("id_cnr") val idCnr: Long? = null,
    @Optional val name: String? = null,
    @Optional @SerialName("loc_map") val locMap: Long? = null,
    @Optional @SerialName("loc_x") val locX: Long? = null,
    @Optional @SerialName("loc_y") val locY: Long? = null,
    @Optional val enabled: Boolean = false,
    @Optional val status: String = "ok"
)

@Serializable
data class ChannelReading(
    val date: LocalDate,
    @SerialName("value_min") val valueMin: Long,
    @Optional @SerialName("value_avg") val valueAvg: Long?,
    @Optional @SerialName("value_max") val valueMax: Long?,
    @Optional val deviation: Long?,
    @Optional val error: Char
)
