package it.cnr.oldmusa

import it.cnr.oldmusa.util.selection.MusaSelectorTree
import it.cnr.oldmusa.util.selection.MusaSelectorTree.SelectionType
import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class SelectorTreeTest {
    @Test
    fun basicUsageTest() {
        val tree = MusaSelectorTree()
        // The following scenario will happen:
        // 100
        // |-110
        // ||-111
        // ||-112
        // |-120
        // ||-122
        // 200
        // |-210
        // ||- 211
        // ||- 212
        // ||- 213
        // |-220
        // ||-221
        // |-230
        // ||-231
        // ||-232
        // ||-233
        // NOTE: the ids don't need to be unique in real cases (eg. a site and a sensor can have the
        // same ids, as long as they're of a different type), in this case the ids are chosen like
        // this for clarity.

        tree.setSite(100, true)
        // Navigate in site 200
        tree.editSite(200, listOf(210, 220, 230)) { site ->
            site.setSensor(210, true)
            site.setSensor(230, true)
            // navigate in sensor 230
            site.editSensor(230, listOf(231, 232, 233)) { sensor ->
                sensor.setChannel(231, false)
                sensor.setChannel(233, false)
            }
        }

        assertTrue(tree.checkChannel(100, 110, 112))
        assertTrue(tree.checkChannel(100, 120, 122))
        assertEquals(SelectionType.SELECTED, tree.checkSensor(100, 110))
        assertEquals(SelectionType.SELECTED, tree.checkSite(100))
        assertEquals(SelectionType.PARTIAL, tree.checkSite(200))
        assertEquals(SelectionType.SELECTED, tree.checkSensor(200, 210))
        assertEquals(SelectionType.UNSELECTED, tree.checkSensor(200, 220))
        assertEquals(SelectionType.PARTIAL, tree.checkSensor(200, 230))
        assertTrue(tree.checkChannel(200, 210, 212))
        assertTrue(tree.checkChannel(200, 230, 232))
        assertFalse(tree.checkChannel(200, 220, 221))
        assertFalse(tree.checkChannel(200, 230, 231))
        assertFalse(tree.checkChannel(200, 230, 233))

        // Unselect 111 while the site 100 is selected (this should propagate up the tree)
        tree.editSite(100, listOf(110, 120)) { site ->
            site.editSensor(110, listOf(111, 112)) { sensor ->
                sensor.setChannel(111, false)
            }
        }

        assertEquals(SelectionType.PARTIAL, tree.checkSite(100))
        assertEquals(SelectionType.PARTIAL, tree.checkSensor(100, 110))
        assertEquals(SelectionType.SELECTED, tree.checkSensor(100, 120))
        assertTrue(tree.checkChannel(100, 110, 112))
        assertTrue(tree.checkChannel(100, 120, 122))
        assertFalse(tree.checkChannel(100, 110, 111))

        // Also unselect 112 and 122, this should unselect all of the site.
        tree.editSite(100, listOf(110, 120)) { site ->
            site.editSensor(120, listOf(122)) { sensor ->
                sensor.setChannel(122, false)
            }
            site.editSensor(110, listOf(111, 112)) { sensor ->
                sensor.setChannel(112, false)
            }
        }

        assertEquals(SelectionType.UNSELECTED, tree.checkSite(100))
    }
}
