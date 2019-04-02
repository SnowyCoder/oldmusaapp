package com.cnr_isac.oldmusa.api

class Sensor(
        api: Api,
        id: Long,
        val siteId: Long,
        var name: String?,
        var locMapId: Long?,
        var locX: Long?,
        var locY: Long?,
        var enabled: Boolean,
        var status: String
) : ApiEntity(api, id) {

    var locMap: MuseMap?
        get() = locMapId?.let { api.getMap(it) }
        set(value) {
            locMapId = value?.id
        }

    val channels: List<Channel>
        get() = api.getSensorChannels(id).map { api.getChannel(it) }

    fun onUpdate(data: ApiSensor) {
        assert(id == data.id)
        assert(siteId == data.siteId)
        this.name = data.name
        this.locMapId = data.locMap
        this.locX = data.locX
        this.locY = data.locY
        this.enabled = data.enabled
        this.status = data.status
    }

    fun serialize(): ApiSensor {
        return ApiSensor(id, siteId, name, locMapId, locX, locY, enabled, status)
    }

    fun commit() {
        api.updateSensor(id, serialize())
    }

    fun delete() {
        api.deleteSensor(id)
    }
}