package com.cnr_isac.oldmusa.api

import com.cnr_isac.oldmusa.util.TimeUtil.ISO_0_OFFSET_DATE_TIME
import kotlinx.serialization.*
import kotlinx.serialization.Optional
import kotlinx.serialization.internal.StringDescriptor
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*

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
data class ApiChannel(
    @Optional val id: Long? = null,
    @Optional @SerialName("sensor_id") val sensorId: Long? = null,
    @Optional @SerialName("id_cnr") val idCnr: String? = null,

    @Optional val name: String? = null,

    @Optional @SerialName("measure_unit") val measureUnit: String? = null,
    @Optional @SerialName("range_min") val rangeMin: Double? = null,
    @Optional @SerialName("range_max") val rangeMax: Double? = null
)

@Serializable
data class ApiSensor(
    @Optional val id: Long? = null,
    @Optional @SerialName("site_id") val siteId: Long? = null,
    @Optional @SerialName("id_cnr") val idCnr: String? = null,
    @Optional val name: String? = null,
    @Optional @SerialName("loc_x") val locX: Long? = null,
    @Optional @SerialName("loc_y") val locY: Long? = null,
    @Optional val enabled: Boolean = false,
    @Optional val status: String? = null
)

@Serializable
data class ChannelReading(
    @Serializable(LocalDateTimeSerializer::class) val date: Date,
    @SerialName("value_min") val valueMin: Double,
    @Optional @SerialName("value_avg") val valueAvg: Double? = null,
    @Optional @SerialName("value_max") val valueMax: Double? = null,
    @Optional val deviation: Double? = null,
    @Optional val error: Char? = null
)

@Serializer(forClass = Date::class)
object LocalDateTimeSerializer : KSerializer<Date> {
    override val descriptor: SerialDescriptor = StringDescriptor.withName("DateSerializer")

    override fun serialize(encoder: Encoder, obj: Date) {
        encoder.encodeString(ISO_0_OFFSET_DATE_TIME.format(obj))
    }

    override fun deserialize(decoder: Decoder): Date {
        return ISO_0_OFFSET_DATE_TIME.parse(decoder.decodeString())
    }
}
