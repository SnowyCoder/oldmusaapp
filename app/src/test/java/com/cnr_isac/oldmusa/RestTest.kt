package com.cnr_isac.oldmusa

import com.cnr_isac.oldmusa.api.ApiRoom
import com.cnr_isac.oldmusa.api.ApiSensor
import com.cnr_isac.oldmusa.api.RestApi
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import org.hamcrest.CoreMatchers.containsString
import org.junit.Assert.*
import org.junit.Ignore
import org.junit.Test
import java.nio.charset.StandardCharsets
import kotlin.test.assertFailsWith


/**
 * Local unit test, which will execute on the development machine (host).
 * This will test the rest client, if you think that this is messy just don't look
 * at the python test side (everything is done directly trough http)
 */
@Ignore// You don't want this test running if there isn't any server
class RestTest {
    val url = "http://localhost:8080/api/"// Server URL
    val api = RestApi.httpRest(url)// Connection type (rest over http)

    @Test
    fun generalTest() {
        // Create museum
        val museum = api.addMuseum()
        museum.name = "testmuse"
        museum.commit()// Commit sends the field changes to the server
        // Check the name change
        assertThat(
                api.conn.connectRest("GET", "museum/${museum.id}"),
                containsString("testmuse")
        )

        // Add room
        val room = museum.addRoom(ApiRoom(name = "testroom"))
        assertEquals("testroom", room.name)

        // Add sensor
        val sensor = museum.addSensor(ApiSensor(name = "testsensor"))
        assertEquals(museum.id, sensor.museumId)
        assertEquals("testsensor", sensor.name)

        // Foreign key tests
        assertFailsWith<Exception> {
            sensor.locMapId = 199999
            sensor.commit()
        }
        sensor.locMapId = null

        // Add map
        val map = museum.addMap()
        // Link it!
        sensor.locMap = map
        sensor.locX = 1234
        sensor.locY = 5678
        sensor.commit()

        val retSensor = Json.parse(ApiSensor.serializer(), api.conn.connectRest("GET", "sensor/${sensor.id}"))
        assertEquals(sensor.locMapId, retSensor.locMap)
        assertEquals(1234L, retSensor.locX)
        assertEquals(5678L, retSensor.locY)

        // Test map image data
        val charset = StandardCharsets.UTF_8
        map.setImage("png image data".byteInputStream(charset))
        assertEquals("png image data", map.getImage().readBytes().toString(charset))

        // Test removes:
        // TODO: check the sensor->map reference on remove
        assertEquals(1, museum.sensors.size)
        sensor.delete()
        assertTrue(museum.sensors.isEmpty())


        assertEquals(1, museum.maps.size)
        map.delete()
        assertTrue(museum.maps.isEmpty())

        assertEquals(1, museum.rooms.size)
        room.delete()
        assertEquals(0, museum.rooms.size)

        museum.delete()
    }
}