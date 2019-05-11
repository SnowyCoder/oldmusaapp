package com.cnr_isac.oldmusa.api.rest

import com.cnr_isac.oldmusa.api.*
import com.cnr_isac.oldmusa.util.TimeUtil.ISO_0_OFFSET_DATE_TIME
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.list
import kotlinx.serialization.serializer
import java.io.InputStream
import java.lang.ref.WeakReference
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.set

class RestApi(val conn: ApiConnession) : Api {
    private val sites: MutableMap<Long, WeakReference<Site>> = HashMap()
    private val sensors: MutableMap<Long, WeakReference<Sensor>> = HashMap()
    private val channels: MutableMap<Long, WeakReference<Channel>> = HashMap()
    private val users: MutableMap<Long, WeakReference<User>> = HashMap()
    private val json = Json(encodeDefaults = false)

    val headers = HashMap<String, String>()


    // Utils

    private fun query(method: String, path: String, content: String? = null, parameters: Map<String, String>? = null): String {
        return conn.connectRest(method, path, parameters, content, headers)
    }

    // ---------------- LOGIN ----------------

    @Serializable
    private class LoginDataResponse(val token: String)

    override fun getCurrentToken(): String? {
        return headers["Token"]
    }

    override fun useToken(token: String?) {
        if (token == null) headers.remove("Token")
        else headers["Token"] = token
    }

    override fun login(username: String, password: String) {
        val raw = conn.connectRest("GET", "token", parameters = mapOf("username" to username, "password" to password))
        val data = json.parse(LoginDataResponse.serializer(), raw)

        useToken(data.token)
    }

    override fun logout() {
        headers.remove("Token")
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
            it.resetLocalData(data)
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

    override fun addUserAccess(userId: Long, siteId: Long) {
        query("POST", "user/$userId/access", json.stringify(ApiId.serializer(), ApiId(siteId)))
    }

    override fun removeUserAccess(userId: Long, siteId: Long) {
        query("DELETE", "user/$userId/access/$siteId")
    }

    override fun addUserContactFCM(userId: Long, token: String) {
        query("PUT", "user/$userId/contact/fcm/$token")
    }

    override fun removeUserContactFCM(userId: Long, token: String) {
        query("DELETE", "user/$userId/contact/fcm/$token")
    }

    override fun getMe(): User {
        return getCacheOrCreateUser(json.parse(ApiUser.serializer(), query("GET", "user_me")))
    }

    // ---------------- SITE ----------------

    override fun getSiteIds(): List<Long> {
        return json.parse(Long.serializer().list, query("GET", "site"))
    }

    override fun getSites(): List<Site> {
        return getSiteIds().map { getSite(it) }
    }

    override fun addSite(data: ApiSite?): Site {
        val content = data?.let { json.stringify(ApiSite.serializer(), it) }
        val res = query("POST", "site", content)
        return getCacheOrCreateSite(json.parse(ApiSite.serializer(), res))
    }


    override fun getSite(id: Long): Site {
        return getCacheOrCreateSite(
            json.parse(
                ApiSite.serializer(),
                query("GET", "site/$id")
            )
        )
    }

    private fun getCacheOrCreateSite(data: ApiSite): Site {
        sites[data.id]?.get()?.let {
            it.resetLocalData(data)
            return it
        }

        val mus = Site(this, data.id!!, data.idCnr, data.name);
        sites[mus.id] = WeakReference(mus)
        return mus
    }

    override fun updateSite(id: Long, data: ApiSite): Site {
        return getCacheOrCreateSite(
            json.parse(
                ApiSite.serializer(),
                query(
                    "PUT",
                    "site/$id",
                    json.stringify(ApiSite.serializer(), data.copy(id = null))
                )
            )
        )
    }

    override fun deleteSite(id: Long) {
        query("DELETE", "site/$id")
    }

    override fun getSiteSensors(siteId: Long): List<Long> {
        return json.parse(
            Long.serializer().list,
            query("GET", "site/$siteId/sensor")
        )
    }

    override fun addSiteSensor(siteId: Long, sensor: ApiSensor?): Sensor {
        val content = sensor?.let { json.stringify(ApiSensor.serializer(), it) }
        val res = query("POST", "site/$siteId/sensor", content)
        return getCacheOrCreateSensor(json.parse(ApiSensor.serializer(), res))
    }

    override fun getSiteMap(id: Long): InputStream? {
        return try {
            conn.connect("GET", "site/$id/map", headers = headers)
        } catch (e: RestException) {
            if (e.code != 404) throw e
            null
        }
    }

    override fun setSiteMap(id: Long, data: InputStream, resize: MapResizeData?) {
        val params = resize?.let {
            mapOf(
                "resize_from_w" to it.fromW.toString(),
                "resize_from_h" to it.fromH.toString(),
                "resize_to_w" to it.toW.toString(),
                "resize_to_h" to it.toH.toString()
            )
        }

        conn.connect("PUT", "site/$id/map", content = data, contentType = "image/png", headers = headers,
            parameters = params)
    }

    override fun deleteSiteMap(id: Long) {
        query("DELETE", "site/$id/map")
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
            it.resetLocalData(data)
            return it
        }

        val sensor = Sensor(
            this, data.id!!, data.siteId!!, data.idCnr, data.name,
            data.locX, data.locY,
            data.enabled, data.status!!
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
                    json.stringify(ApiSensor.serializer(), data.copy(id = null, siteId = null, status = null))
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
            it.resetLocalData(data)
            return it
        }

        val channel = Channel(this, data.id!!, data.sensorId!!, data.idCnr, data.name, data.measureUnit, data.rangeMin, data.rangeMax);
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

    override fun getChannelReadings(channelId: Long, start: Date, end: Date, precision: String): List<ChannelReading> {
        val res = query("GET", "channel/$channelId/readings", parameters = mapOf(
            "start" to ISO_0_OFFSET_DATE_TIME.format(start),
            "end" to ISO_0_OFFSET_DATE_TIME.format(end),
            "precision" to precision
        ))
        return json.parse(
            ChannelReading.serializer().list,
            res
        )
    }

    companion object {
        private const val TAG = "RestApi"

        fun httpRest(url: String): RestApi {
            return RestApi(HttpApiConnession(url))
        }
    }
}
