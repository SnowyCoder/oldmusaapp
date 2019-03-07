package com.cnr_isac.oldmusa.api

import com.cnr_isac.oldmusa.api.rest.ApiConnession
import com.cnr_isac.oldmusa.api.rest.HttpApiConnession
import kotlinx.serialization.json.Json
import kotlinx.serialization.list
import kotlinx.serialization.serializer
import java.io.InputStream
import java.lang.ref.WeakReference

class RestApi(val conn: ApiConnession) : Api {
    private val museums: MutableMap<Long, WeakReference<Museum>> = HashMap()
    private val rooms: MutableMap<Long, WeakReference<Room>> = HashMap()
    private val sensors: MutableMap<Long, WeakReference<Sensor>> = HashMap()
    private val maps: MutableMap<Long, WeakReference<MuseMap>> = HashMap()
    private val json = Json(encodeDefaults = false)


    // ---------------- MUSEUM ----------------

    override fun getMuseumIds(): List<Long> {
        return json.parse(Long.serializer().list, conn.connectRest("GET", "museum"))
    }

    override fun getMuseums(): List<Museum> {
        return getMuseumIds().map { getMuseum(it) }
    }

    override fun addMuseum(data: ApiMuseum?): Museum {
        val content = data?.let { json.stringify(ApiMuseum.serializer(), it) }
        val res = conn.connectRest("POST", "museum", content = content)
        return getCacheOrCreateMuseum(json.parse(ApiMuseum.serializer(), res))
    }


    override fun getMuseum(id: Long): Museum {
        return getCacheOrCreateMuseum(
                json.parse(
                        ApiMuseum.serializer(),
                        conn.connectRest("GET", "museum/$id")
                )
        )
    }

    private fun getCacheOrCreateMuseum(data: ApiMuseum): Museum {
        museums[data.id]?.get()?.let {
            it.onUpdate(data)
            return it
        }

        val mus = Museum(this, data.id!!, data.name);
        museums[mus.id] = WeakReference(mus)
        return mus
    }

    override fun updateMuseum(id: Long, data: ApiMuseum): Museum {
        return getCacheOrCreateMuseum(
                json.parse(
                        ApiMuseum.serializer(),
                        conn.connectRest(
                                "PUT",
                                "museum/$id",
                                json.stringify(ApiMuseum.serializer(), data.copy(id=null))
                        )
                )
        )
    }

    override fun deleteMuseum(id: Long) {
        conn.connectRest("DELETE", "museum/$id")
    }

    override fun getMuseumSensors(museumId: Long): List<Long> {
        return json.parse(
                Long.serializer().list,
                conn.connectRest("GET", "museum/$museumId/sensor")
        )
    }

    override fun addMuseumSensor(museumId: Long, sensor: ApiSensor?): Sensor {
        val content = sensor?.let { json.stringify(ApiSensor.serializer(), it) }
        val res = conn.connectRest("POST", "museum/$museumId/sensor", content = content)
        return getCacheOrCreateSensor(json.parse(ApiSensor.serializer(), res))
    }

    override fun getMuseumRooms(museumId: Long): List<Long> {
        return json.parse(
                Long.serializer().list,
                conn.connectRest("GET", "museum/$museumId/room")
        )
    }

    override fun addMuseumRoom(museumId: Long, room: ApiRoom?): Room {
        val content = room?.let { json.stringify(ApiRoom.serializer(), it) }
        val res = conn.connectRest("POST", "museum/$museumId/room", content = content)
        return getCacheOrCreateRoom(json.parse(ApiRoom.serializer(), res))
    }

    override fun getMuseumMaps(museumId: Long): List<Long> {
        return json.parse(
                Long.serializer().list,
                conn.connectRest("GET", "museum/$museumId/map")
        )
    }

    override fun addMuseumMap(museumId: Long, map: ApiMap?): MuseMap {
        val content = map?.let { json.stringify(ApiMap.serializer(), it) }
        val res = conn.connectRest("POST", "museum/$museumId/map", content = content)
        return getCacheOrCreateMap(json.parse(ApiMap.serializer(), res))
    }

    // ---------------- MAP ----------------

    override fun getMap(id: Long): MuseMap {
        return getCacheOrCreateMap(
                json.parse(
                        ApiMap.serializer(),
                        conn.connectRest("GET", "map/$id")
                )
        )
    }

    override fun getMapImage(id: Long): InputStream {
        return conn.connect("GET", "map/$id/image")
    }

    override fun setMapImage(id: Long, data: InputStream) {
        conn.connect("PUT", "map/$id/image", content = data, contentType = "image/png")
    }

    override fun getMapSensors(id: Long): List<Long> {
        return json.parse(
                Long.serializer().list,
                conn.connectRest("GET", "map/$id/sensor")
        )
    }

    private fun getCacheOrCreateMap(data: ApiMap): MuseMap {
        maps[data.id]?.get()?.let {
            it.onUpdate(data)
            return it
        }

        val map = MuseMap(this, data.id!!, data.museumId!!);
        maps[map.id] = WeakReference(map)
        return map
    }

    override fun updateMap(id: Long, data: ApiMap): MuseMap {
        return getCacheOrCreateMap(
                json.parse(
                        ApiMap.serializer(),
                        conn.connectRest(
                                "PUT",
                                "map/$id",
                                json.stringify(ApiMap.serializer(), data.copy(id = null, museumId = null))
                        )
                )
        )
    }

    override fun deleteMap(id: Long) {
        conn.connectRest("DELETE", "map/$id")
    }


    // ---------------- ROOM ----------------

    override fun getRoom(id: Long): Room {
        return getCacheOrCreateRoom(
                json.parse(
                        ApiRoom.serializer(),
                        conn.connectRest("GET", "room/$id")
                )
        )
    }

    private fun getCacheOrCreateRoom(data: ApiRoom): Room {
        rooms[data.id]?.get()?.let {
            it.onUpdate(data)
            return it
        }

        val room = Room(this, data.id!!, data.museumId!!, data.name);
        rooms[room.id] = WeakReference(room)
        return room
    }

    override fun updateRoom(id: Long, data: ApiRoom): Room {
        return getCacheOrCreateRoom(
                json.parse(
                        ApiRoom.serializer(),
                        conn.connectRest(
                                "PUT",
                                "room/$id",
                                json.stringify(ApiRoom.serializer(), data.copy(id = null, museumId = null))
                        )
                )
        )
    }

    override fun deleteRoom(id: Long) {
        conn.connectRest("DELETE", "room/$id")
    }

    // ---------------- SENSOR ----------------

    override fun getSensor(id: Long): Sensor {
        return getCacheOrCreateSensor(
                json.parse(
                        ApiSensor.serializer(),
                        conn.connectRest("GET", "sensor/$id")
                )
        )
    }

    private fun getCacheOrCreateSensor(data: ApiSensor): Sensor {
        sensors[data.id]?.get()?.let {
            it.onUpdate(data)
            return it
        }

        val sensor = Sensor(this, data.id!!, data.museumId!!, data.name, data.room,
                data.rangeMin, data.rangeMax, data.locMap, data.locX, data.locY,
                data.enabled, data.status)
        sensors[sensor.id] = WeakReference(sensor)
        return sensor
    }

    override fun updateSensor(id: Long, data: ApiSensor): Sensor {
        return getCacheOrCreateSensor(
                json.parse(
                        ApiSensor.serializer(),
                        conn.connectRest(
                                "PUT",
                                "sensor/$id",
                                json.stringify(ApiSensor.serializer(), data.copy(id = null, museumId = null))
                        )
                )
        )
    }

    override fun deleteSensor(id: Long) {
        conn.connectRest("DELETE", "sensor/$id")
    }

    companion object {
        fun httpRest(url: String): RestApi {
            return RestApi(HttpApiConnession(url))
        }
    }
}
