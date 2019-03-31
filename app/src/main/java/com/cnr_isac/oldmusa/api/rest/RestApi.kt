package com.cnr_isac.oldmusa.api.rest

import android.util.Log
import com.cnr_isac.oldmusa.api.*
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.lang.ref.WeakReference

class RestApi(val conn: ApiConnession) : Api {
    private val museums: MutableMap<Long, WeakReference<Museum>> = HashMap()
    private val sensors: MutableMap<Long, WeakReference<Sensor>> = HashMap()
    private val channels: MutableMap<Long, WeakReference<Channel>> = HashMap()
    private val maps: MutableMap<Long, WeakReference<MuseMap>> = HashMap()
    private val users: MutableMap<Long, WeakReference<User>> = HashMap()
    private val json = Json(encodeDefaults = false)

    val headers = HashMap<String, String>()
    private var tokenExpirationDate: Long? = null

    private var lastUsername: String? = null
    private var lastPassword: String? = null


    // Utils

    private fun query(method: String, path: String, content: String? = null, parameters: Map<String, String>? = null): String {
        ensureTokenValidity()
        return conn.connectRest(method, path, parameters, content, headers)
    }

    // ---------------- LOGIN ----------------

    @Serializable
    private class LoginData(val username: String, val password: String)

    @Serializable
    private class LoginDataResponse(val token: String, val duration: Long)

    override fun login(username: String, password: String) {
        val raw = conn.connectRest("GET", "token", parameters = mapOf("username" to username, "password" to password))
        val data = json.parse(LoginDataResponse.serializer(), raw)

        tokenExpirationDate = System.currentTimeMillis() + data.duration * 1000 - 10;
        Log.i(TAG, "Token accepted, user: $username, duration: ${data.duration}")

        headers["Token"] = data.token
        lastUsername = username
        lastPassword = password
    }

    override fun logout() {
        headers.remove("Token")
        tokenExpirationDate = null
        lastUsername = null
        lastPassword = null
    }

    private fun ensureTokenValidity() {
        val expiration = tokenExpirationDate
        val user = lastUsername
        val passw = lastPassword

        if (expiration == null || user == null || passw == null) return
        if (System.currentTimeMillis() < expiration) return

        // TODO: login retry logic
        login(user, passw)
    }

    // ---------------- USER ----------------

    override fun getUserIds(): List<Long> {
        return json.parse(Long.serializer().list, query("GET", "user"))
    }

    override fun getUsers(): List<User> {
        return getUserIds().map { getUser(it) }
    }

    override fun addUser(data: ApiUser): User {
        val content = data.let { json.stringify(ApiUser.serializer(), it) }
        val res = query("POST", "user", content)
        return getCacheOrCreateUser(json.parse(ApiUser.serializer(), res))
    }

    override fun getUser(id: Long): User {
        return getCacheOrCreateUser(
            json.parse(
                ApiUser.serializer(),
                query("GET", "user/$id")
            )
        )
    }

    private fun getCacheOrCreateUser(data: ApiUser): User {
        users[data.id]?.get()?.let {
            it.onUpdate(data)
            return it
        }

        val user = User(this, data.id!!, data.username!!, null, data.permission!!);
        users[user.id] = WeakReference(user)
        return user
    }

    override fun updateUser(id: Long, data: ApiUser): User {
        return getCacheOrCreateUser(
            json.parse(
                ApiUser.serializer(),
                query(
                    "PUT",
                    "user/$id",
                    json.stringify(ApiUser.serializer(), data.copy(id = null))
                )
            )
        )
    }

    override fun deleteUser(id: Long) {
        query("DELETE", "user/$id")
    }

    override fun getUserAccessIds(userId: Long): List<Long> {
        return json.parse(Long.serializer().list, query("GET", "user/$userId/access"))
    }

    override fun addUserAccess(userId: Long, museumId: Long) {
        query("POST", "user/$userId/access", json.stringify(ApiId.serializer(), ApiId(museumId)))
    }

    override fun removeUserAccess(userId: Long, museumId: Long) {
        query("DELETE", "user/$userId/access", json.stringify(ApiId.serializer(), ApiId(museumId)))
    }

    // ---------------- MUSEUM ----------------

    override fun getMuseumIds(): List<Long> {
        return json.parse(Long.serializer().list, query("GET", "museum"))
    }

    override fun getMuseums(): List<Museum> {
        return getMuseumIds().map { getMuseum(it) }
    }

    override fun addMuseum(data: ApiMuseum?): Museum {
        val content = data?.let { json.stringify(ApiMuseum.serializer(), it) }
        val res = query("POST", "museum", content)
        return getCacheOrCreateMuseum(json.parse(ApiMuseum.serializer(), res))
    }


    override fun getMuseum(id: Long): Museum {
        return getCacheOrCreateMuseum(
            json.parse(
                ApiMuseum.serializer(),
                query("GET", "museum/$id")
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
                query(
                    "PUT",
                    "museum/$id",
                    json.stringify(ApiMuseum.serializer(), data.copy(id = null))
                )
            )
        )
    }

    override fun deleteMuseum(id: Long) {
        query("DELETE", "museum/$id")
    }

    override fun getMuseumSensors(museumId: Long): List<Long> {
        return json.parse(
            Long.serializer().list,
            query("GET", "museum/$museumId/sensor")
        )
    }

    override fun addMuseumSensor(museumId: Long, sensor: ApiSensor?): Sensor {
        val content = sensor?.let { json.stringify(ApiSensor.serializer(), it) }
        val res = query("POST", "museum/$museumId/sensor", content)
        return getCacheOrCreateSensor(json.parse(ApiSensor.serializer(), res))
    }

    override fun getMuseumMaps(museumId: Long): List<Long> {
        return json.parse(
            Long.serializer().list,
            query("GET", "museum/$museumId/map")
        )
    }

    override fun addMuseumMap(museumId: Long, map: ApiMap?): MuseMap {
        val content = map?.let { json.stringify(ApiMap.serializer(), it) }
        val res = query("POST", "museum/$museumId/map", content)
        return getCacheOrCreateMap(json.parse(ApiMap.serializer(), res))
    }

    // ---------------- MAP ----------------

    override fun getMap(id: Long): MuseMap {
        return getCacheOrCreateMap(
            json.parse(
                ApiMap.serializer(),
                query("GET", "map/$id")
            )
        )
    }

    override fun getMapImage(id: Long): InputStream {
        return conn.connect("GET", "map/$id/image", headers = headers)
    }

    override fun setMapImage(id: Long, data: InputStream) {
        conn.connect("PUT", "map/$id/image", content = data, contentType = "image/png", headers = headers)
    }

    override fun getMapSensors(id: Long): List<Long> {
        return json.parse(
            Long.serializer().list,
            query("GET", "map/$id/sensor")
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
                query(
                    "PUT",
                    "map/$id",
                    json.stringify(ApiMap.serializer(), data.copy(id = null, museumId = null))
                )
            )
        )
    }

    override fun deleteMap(id: Long) {
        query("DELETE", "map/$id")
    }

    // ---------------- SENSOR ----------------

    override fun getSensor(id: Long): Sensor {
        return getCacheOrCreateSensor(
            json.parse(
                ApiSensor.serializer(),
                query("GET", "sensor/$id")
            )
        )
    }

    override fun addSensorChannel(sensorId: Long, data: ApiChannel?): Channel {
        val content = data?.let { json.stringify(ApiChannel.serializer(), it) }
        val res = query("POST", "sensor/$sensorId/channel", content)
        return getCacheOrCreateChannel(json.parse(ApiChannel.serializer(), res))
    }

    override fun getSensorChannels(sensorId: Long): List<Long> {
        return json.parse(
            Long.serializer().list,
            query("GET", "sensor/$sensorId/channel")
        )
    }

    private fun getCacheOrCreateSensor(data: ApiSensor): Sensor {
        sensors[data.id]?.get()?.let {
            it.onUpdate(data)
            return it
        }

        val sensor = Sensor(
            this, data.id!!, data.museumId!!, data.name,
            data.locMap, data.locX, data.locY,
            data.enabled, data.status
        )
        sensors[sensor.id] = WeakReference(sensor)
        return sensor
    }

    override fun updateSensor(id: Long, data: ApiSensor): Sensor {
        return getCacheOrCreateSensor(
            json.parse(
                ApiSensor.serializer(),
                query(
                    "PUT",
                    "sensor/$id",
                    json.stringify(ApiSensor.serializer(), data.copy(id = null, museumId = null))
                )
            )
        )
    }

    override fun deleteSensor(id: Long) {
        query("DELETE", "sensor/$id")
    }

    // ---------------- CHANNEL ----------------

    override fun getChannel(id: Long): Channel {
        return getCacheOrCreateChannel(
            json.parse(
                ApiChannel.serializer(),
                query("GET", "channel/$id")
            )
        )
    }

    private fun getCacheOrCreateChannel(data: ApiChannel): Channel {
        channels[data.id]?.get()?.let {
            it.onUpdate(data)
            return it
        }

        val channel = Channel(this, data.id!!, data.sensorId!!, data.cnrId, data.name, data.measureUnit, data.rangeMin, data.rangeMax);
        channels[channel.id] = WeakReference(channel)
        return channel
    }

    override fun updateChannel(id: Long, data: ApiChannel): Channel {
        return getCacheOrCreateChannel(
            json.parse(
                ApiChannel.serializer(),
                query(
                    "PUT",
                    "channel/$id",
                    json.stringify(ApiChannel.serializer(), data.copy(id = null, sensorId = null))
                )
            )
        )
    }

    override fun deleteChannel(id: Long) {
        query("DELETE", "channel/$id")
    }

    companion object {
        private const val TAG = "RestApi"

        fun httpRest(url: String): RestApi {
            return RestApi(HttpApiConnession(url))
        }
    }
}
