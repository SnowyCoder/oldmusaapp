package com.cnr_isac.oldmusa.api

import java.time.LocalDateTime
import java.util.*

class Channel(
    api: Api,
    id: Long,
    val sensorId: Long,
    val idCnr: String?,
    var name: String?,
    var measureUnit: String?,
    var rangeMin: Double?,
    var rangeMax: Double?
) : ApiEntity(api, id) {


    fun getReadings(start: Date, end: Date, precision: String = "atomic"): List<ChannelReading> {
        return api.getChannelReadings(id, start, end)
    }

    fun onUpdate(data: ApiChannel) {
        assert(id == data.id)
        assert(sensorId == data.sensorId)
        assert(idCnr == data.idCnr)
        this.name = data.name
        this.measureUnit = data.measureUnit
        this.rangeMin = data.rangeMin
        this.rangeMax = data.rangeMax
    }

    fun serialize(): ApiChannel {
        return ApiChannel(id, sensorId, idCnr, name, measureUnit, rangeMin, rangeMax)
    }

    fun commit() {
        api.updateChannel(id, serialize())
    }

    fun delete() {
        api.deleteChannel(id)
    }
}