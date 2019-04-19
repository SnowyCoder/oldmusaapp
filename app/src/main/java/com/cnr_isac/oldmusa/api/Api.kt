package com.cnr_isac.oldmusa.api

import java.io.InputStream
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Anything that can answer the API requests,
 * This is interface is usually implemented in RestApi, but might be replaced by
 * another class in a simulation environment.
 * The purpose of this class is not to provide a high level interface (that is done by Site,
 * Channel, Sensor... classes) but to provide a lower level data source that will be used to
 * construct the higher level APIs.
 */
interface Api {

    // ---------------- LOGIN ----------------

    fun useToken(token: String?)

    fun getCurrentToken(): String?

    fun login(username: String, password: String)

    fun logout()

    // ---------------- USER ----------------

    fun getUserIds(): List<Long>

    fun getUsers(): List<User>

    fun addUser(data: ApiUser): User

    fun getUser(id: Long): User

    fun updateUser(id: Long, data: ApiUser): User

    fun deleteUser(id: Long)

    fun getUserAccessIds(userId: Long): List<Long>

    fun addUserAccess(userId: Long, siteId: Long)

    fun removeUserAccess(userId: Long, siteId: Long)

    fun addUserContactFCM(userId: Long, token: String)

    fun removeUserContactFCM(userId: Long, token: String)

    fun getMe(): User

    // ---------------- SITE ----------------

    fun getSiteIds(): List<Long>

    fun getSites(): List<Site>

    fun addSite(data: ApiSite? = null): Site

    fun getSite(id: Long): Site

    fun getSiteSensors(siteId: Long): List<Long>

    fun addSiteSensor(siteId: Long, sensor: ApiSensor?): Sensor

    fun getSiteMap(id: Long): InputStream?

    fun setSiteMap(id: Long, data: InputStream)

    fun deleteSiteMap(id: Long)

    fun updateSite(id: Long, data: ApiSite): Site

    fun deleteSite(id: Long)

    // ---------------- SENSOR ----------------

    fun getSensor(id: Long): Sensor

    fun updateSensor(id: Long, data: ApiSensor): Sensor

    fun deleteSensor(id: Long)

    fun addSensorChannel(sensorId: Long, data: ApiChannel?): Channel

    fun getSensorChannels(sensorId: Long): List<Long>


    // ---------------- CHANNEL ----------------

    fun getChannel(id: Long): Channel

    fun updateChannel(id: Long, data: ApiChannel): Channel

    fun deleteChannel(id: Long)

    fun getChannelReadings(channelId: Long, start: LocalDateTime, end: LocalDateTime, precision: String = "atomic"): List<ChannelReading>
}