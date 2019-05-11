package com.cnr_isac.oldmusa.api

import java.io.InputStream

class Site(api: Api, id: Long, var idCnr: String?, var name: String?) : ApiEntity(api, id) {

    val sensors: List<Sensor>
        get() = api.getSiteSensors(id).map { api.getSensor(it) }

    fun addSensor(data: ApiSensor? = null) = api.addSiteSensor(id, data)

    fun getMap(): InputStream? {
        return api.getSiteMap(id)
    }

    fun setMap(os: InputStream, resize: MapResizeData? = null) {
        api.setSiteMap(id, os, resize)
    }

    fun deleteMap() {
        api.deleteSiteMap(id)
    }

    fun resetLocalData(data: ApiSite) {
        assert(id == data.id)
        this.name = data.name
        this.idCnr = data.idCnr
    }

    fun serialize(): ApiSite {
        return ApiSite(id, idCnr, name)
    }

    fun commit() {
        api.updateSite(id, serialize())
    }

    fun delete() {
        api.deleteSite(id)
    }
}
