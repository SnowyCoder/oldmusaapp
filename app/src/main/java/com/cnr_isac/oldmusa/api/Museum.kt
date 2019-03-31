package com.cnr_isac.oldmusa.api

class Museum(api: Api, id: Long, var name: String?) : ApiEntity(api, id) {

    val sensors: List<Sensor>
        get() = api.getMuseumSensors(id).map { api.getSensor(it) }

    val maps: List<MuseMap>
        get() = api.getMuseumMaps(id).map { api.getMap(it) }


    fun addSensor(data: ApiSensor? = null) = api.addMuseumSensor(id, data)

    fun addMap(data: ApiMap? = null) = api.addMuseumMap(id, data)

    fun onUpdate(data: ApiMuseum) {
        assert(id == data.id)
        this.name = data.name
    }

    fun serialize(): ApiMuseum {
        return ApiMuseum(id, name)
    }

    fun commit() {
        api.updateMuseum(id, serialize())
    }

    fun delete() {
        api.deleteMuseum(id)
    }
}
