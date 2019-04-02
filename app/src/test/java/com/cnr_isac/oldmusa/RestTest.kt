package com.cnr_isac.oldmusa

import com.cnr_isac.oldmusa.api.ApiSensor
import com.cnr_isac.oldmusa.api.ApiUser
import com.cnr_isac.oldmusa.api.RestException
import com.cnr_isac.oldmusa.api.rest.RestApi
import kotlinx.serialization.json.Json
import org.hamcrest.CoreMatchers.containsString
import org.junit.Assert.*
import org.junit.Ignore
import org.junit.Test
import java.io.FileNotFoundException
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

    val rootPassword = System.getenv("ROOT_PASSWORD") ?: "password"

    @Test
    fun generalTest() {
        api.login("root", rootPassword)

        val headers = api.headers

        // Create site
        val site = api.addSite()
        site.name = "testmuse"
        site.commit()// Commit sends the field changes to the server
        // Check the name change
        assertThat(
                api.conn.connectRest("GET", "site/${site.id}", headers = headers),
                containsString("testmuse")
        )

        // Add sensor
        val sensor = site.addSensor(ApiSensor(name = "testsensor"))
        assertEquals(site.id, sensor.siteId)
        assertEquals("testsensor", sensor.name)

        // Foreign key tests
        assertFailsWith<RestException> {
            sensor.locMapId = 199999
            sensor.commit()
        }
        sensor.locMapId = null

        // Add map
        val map = site.addMap()
        // Link it!
        sensor.locMap = map
        sensor.locX = 1234
        sensor.locY = 5678
        sensor.commit()

        val retSensor = Json.parse(ApiSensor.serializer(), api.conn.connectRest("GET", "sensor/${sensor.id}", headers = headers))
        assertEquals(sensor.locMapId, retSensor.locMap)
        assertEquals(1234L, retSensor.locX)
        assertEquals(5678L, retSensor.locY)

        // Test map image data
        val charset = StandardCharsets.UTF_8
        map.setImage("png image data".byteInputStream(charset))
        assertEquals("png image data", map.getImage().readBytes().toString(charset))

        // Test removes:
        // TODO: check the sensor->map reference on remove
        assertEquals(1, site.sensors.size)
        sensor.delete()
        assertTrue(site.sensors.isEmpty())


        assertEquals(1, site.maps.size)
        map.delete()
        assertTrue(site.maps.isEmpty())

        site.delete()
    }

    @Test
    fun permissionViewTest() {
        api.login("root", rootPassword)

        val mus1 = api.addSite()
        val mus2 = api.addSite()
        val mus3 = api.addSite()

        val user = api.addUser(ApiUser(username = "paolo", password = "123"))
        user.addAccess(mus1)
        user.addAccess(mus2)

        assertTrue("Cannot see every site", api.getSites().containsAll(listOf(mus1, mus2, mus3)))

        api.logout()
        api.login("paolo", "123")

        assertEquals(api.getSites(), listOf(mus1, mus2))

        // The user cannot see the third site so it throws 404
        assertFailsWith<RestException> {
            api.getSite(mus3.id)
        }

        assertEquals(user, api.getMe())

        api.logout()
        api.login("root", rootPassword)

        user.delete()
    }
}