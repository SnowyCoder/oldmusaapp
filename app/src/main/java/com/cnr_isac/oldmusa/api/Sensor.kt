package com.cnr_isac.oldmusa.api

class Sensor(
        api: Api,
        id: Long,
        val museumId: Long,
        var name: String?,
        var roomId: Long?,
        var rangeMin: Long?,
        var rangeMax: Long?,
        var locMapId: Long?,
        var locX: Long?,
        var locY: Long?,
        var enabled: Boolean,
        var status: String
) : ApiEntity(api, id) {

    var room: Room?
        get() {
            return roomId?.let { api.getRoom(it) }
        }
        set(value) {
            roomId = value?.id
        }

    var locMap: MuseMap?
        get() = locMapId?.let { api.getMap(it) }
        set(value) {
            locMapId = locMap?.id
        }

    fun onUpdate(data: ApiSensor) {
        assert(id == data.id)
        assert(museumId == data.museumId)
        this.name = data.name
        this.roomId = data.room
        this.rangeMin = data.rangeMin
        this.rangeMax = data.rangeMax
        this.locMapId = data.locMap
        this.locX = data.locX
        this.locY = data.locY
        this.enabled = data.enabled
        this.status = data.status
    }

    fun serialize(): ApiSensor {
        return ApiSensor(id, museumId, name, roomId, rangeMin, rangeMax, locMapId, locX, locY, enabled, status)
    }

    fun commit() {
        api.updateSensor(id, serialize())
    }

    fun delete() {
        api.deleteSensor(id)
    }
}