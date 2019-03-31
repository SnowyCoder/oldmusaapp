package com.cnr_isac.oldmusa.api

import java.io.InputStream

/**
 * Anything that can answer the API requests,
 * This is interface is usually implemented in RestApi, but might be replaced by
 * another class in a simulation environment.
 * The purpose of this class is not to provide a high level interface (that is done by Museum,
 * Channel, Sensor... classes) but to provide a lower level data source that will be used to
 * construct the higher level APIs.
 */
interface Api {

    // ---------------- LOGIN ----------------

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

    fun addUserAccess(userId: Long, museumId: Long)

    fun removeUserAccess(userId: Long, museumId: Long)

    // ---------------- MUSEUM ----------------

    fun getMuseumIds(): List<Long>

    fun getMuseums(): List<Museum>

    fun addMuseum(data: ApiMuseum? = null): Museum

    fun getMuseum(id: Long): Museum

    fun getMuseumSensors(museumId: Long): List<Long>

    fun addMuseumSensor(museumId: Long, sensor: ApiSensor?): Sensor

    fun getMuseumMaps(museumId: Long): List<Long>

    fun addMuseumMap(museumId: Long, map: ApiMap?): MuseMap

    fun updateMuseum(id: Long, data: ApiMuseum): Museum

    fun deleteMuseum(id: Long)

    // ---------------- MAP ----------------

    fun getMap(id: Long): MuseMap

    fun getMapImage(id: Long): InputStream

    fun setMapImage(id: Long, data: InputStream)

    fun getMapSensors(id: Long): List<Long>

    fun updateMap(id: Long, data: ApiMap): MuseMap

    fun deleteMap(id: Long)


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

}