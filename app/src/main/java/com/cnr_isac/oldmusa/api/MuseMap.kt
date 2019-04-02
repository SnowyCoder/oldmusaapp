package com.cnr_isac.oldmusa.api

import java.io.InputStream

class MuseMap(api: Api, id: Long, val siteId: Long) : ApiEntity(api, id) {

    val sensors: List<Sensor>
        get() = api.getMapSensors(id).map { api.getSensor(it) }

    fun getImage(): InputStream {
        return api.getMapImage(id)
    }

    fun setImage(os: InputStream) {
        api.setMapImage(id, os)
    }


    fun onUpdate(data: ApiMap) {
        assert(id == data.id)
        assert(siteId == data.siteId)
    }

    fun serialize(): ApiMap {
        return ApiMap(id, siteId)
    }

    fun commit() {
        api.updateMap(id, serialize())
    }

    fun delete() {
        api.deleteMap(id)
    }
}