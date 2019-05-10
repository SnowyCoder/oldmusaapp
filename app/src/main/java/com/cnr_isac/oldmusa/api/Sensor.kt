package com.cnr_isac.oldmusa.api

class Sensor(
        api: Api,
        id: Long,
        val siteId: Long,
        var idCnr: String?,
        var name: String?,
        var locX: Long?,
        var locY: Long?,
        var enabled: Boolean,
        var status: String
) : ApiEntity(api, id) {
    val channels: List<Channel>
        get() = api.getSensorChannels(id).map { api.getChannel(it) }

    fun resetLocalData(data: ApiSensor) {
        assert(id == data.id)
        assert(siteId == data.siteId)
        this.name = data.name
        this.locX = data.locX
        this.locY = data.locY
        this.enabled = data.enabled
        this.status = data.status!!
    }

    fun serialize(): ApiSensor {
        return ApiSensor(id, siteId, idCnr, name, locX, locY, enabled, status)
    }

    fun commit() {
        api.updateSensor(id, serialize())
    }

    fun delete() {
        api.deleteSensor(id)
    }

    fun addChannel(data: ApiChannel? = null) = api.addSensorChannel(id, data)
}