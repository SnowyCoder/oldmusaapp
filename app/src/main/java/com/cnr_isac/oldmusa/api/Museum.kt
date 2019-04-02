package com.cnr_isac.oldmusa.api

class Site(api: Api, id: Long, var name: String?) : ApiEntity(api, id) {

    val sensors: List<Sensor>
        get() = api.getSiteSensors(id).map { api.getSensor(it) }

    val maps: List<MuseMap>
        get() = api.getSiteMaps(id).map { api.getMap(it) }


    fun addSensor(data: ApiSensor? = null) = api.addSiteSensor(id, data)

    fun addMap(data: ApiMap? = null) = api.addSiteMap(id, data)

    fun onUpdate(data: ApiSite) {
        assert(id == data.id)
        this.name = data.name
    }

    fun serialize(): ApiSite {
        return ApiSite(id, name)
    }

    fun commit() {
        api.updateSite(id, serialize())
    }

    fun delete() {
        api.deleteSite(id)
    }
}
