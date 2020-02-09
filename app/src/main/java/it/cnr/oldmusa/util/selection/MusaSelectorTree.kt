package it.cnr.oldmusa.util.selection

/**
 * This is somewhat of a strange data structure, it's a lazy selection tree.
 * The tree should know if a site/sensor/channel is seleced (or partially selected) and it should
 * also be able to edit this information, propagating the selections and maintaining the lazyness.
 *
 * It's a simple operation but it needs a bit of constraints and rules to be followed, some unit
 * tests ensure its functionality.
 */
class MusaSelectorTree {
    val sites: MutableMap<Int, SelectorTreeSite> = HashMap()


    class SelectorTreeSite(val tree: MusaSelectorTree, val siteId: Int) {
        var selected: Boolean = false
        private var sensors: MutableMap<Int, SelectorTreeSensor> = HashMap()
        private var allSensors: List<Int> = emptyList()

        val selection: SelectionType
            get() {
                return when {
                    selected -> SelectionType.SELECTED
                    sensors.isNotEmpty() -> SelectionType.PARTIAL
                    else -> SelectionType.UNSELECTED
                }
            }

        fun listSensors(): Collection<SelectorTreeSensor> = sensors.values

        fun checkSensor(sensorId: Int): SelectionType {
            if (selected) return SelectionType.SELECTED
            return getSensor(sensorId)?.selection ?: SelectionType.UNSELECTED
        }

        fun checkChannel(sensorId: Int, channelId: Int): Boolean {
            if (selected) return true
            return getSensor(sensorId)?.checkChannel(channelId) ?: false
        }

        fun startEdit(sensors: List<Int>) {
            allSensors = sensors
        }

        fun getSensor(sensorId: Int): SelectorTreeSensor? {
            return sensors[sensorId]
        }

        fun getOrCreateSensor(sensorId: Int): SelectorTreeSensor {
            var sensor = getSensor(sensorId)
            if (sensor != null) return sensor

            if (this.selected) {
                this.selected = false
                this.allSensors.forEach { sid ->
                    this.sensors[sid] = SelectorTreeSensor(
                        this,
                        sid
                    ).apply {
                        select()
                    }
                }
                sensor = this.sensors[sensorId]!!
            } else {
                sensor =
                    SelectorTreeSensor(
                        this,
                        sensorId
                    )
                this.sensors[sensorId] = sensor
            }

            return sensor
        }

        fun setSensor(sensorId: Int, selected: Boolean) {
            if (selected) {
                val sensor = getOrCreateSensor(sensorId)
                sensor.select()
            } else {
                if (this.selected) {
                    this.selected = false

                    this.allSensors
                        .filter { it != sensorId }
                        .forEach { id ->
                            val sensor =
                                SelectorTreeSensor(
                                    this,
                                    id
                                )
                            sensor.select()
                            this.sensors[sensorId] = sensor
                        }
                } else {
                    sensors.remove(sensorId)
                }
            }
        }

        fun select() {
            this.selected = true
            this.sensors.clear()
        }

        fun stopEdit() {
            if (sensors.size == allSensors.size) {
                if (sensors.values.all { it.selected }) {
                    sensors.clear()
                    selected = true
                }
            } else if (sensors.isEmpty() && !selected) {
                tree.sites.remove(siteId)
            }
        }

        inline fun editSensor(sensorId: Int, channels: List<Int>, action: (SelectorTreeSensor) -> Unit) {
            val site = getOrCreateSensor(sensorId)
            site.startEdit(channels)
            action(site)
            site.stopEdit()
        }

        fun copy(tree: MusaSelectorTree): SelectorTreeSite {
            val other = SelectorTreeSite(tree, siteId)
            other.selected = this.selected
            for (sensor in this.sensors.values) {
                other.sensors[sensor.sensorId] = sensor.copy(other)
            }
            return other
        }
    }

    class SelectorTreeSensor(val site: SelectorTreeSite, val sensorId: Int) {
        var selected: Boolean = false
        private var channels: MutableSet<Int> = HashSet()
        private var allChannels: Set<Int> = HashSet()

        val selection: SelectionType
            get() {
                return when {
                    selected -> SelectionType.SELECTED
                    channels.isNotEmpty() -> SelectionType.PARTIAL
                    else -> SelectionType.UNSELECTED
                }
            }


        fun listChannels(): Collection<Int> = channels

        fun checkChannel(channelId: Int): Boolean {
            if (selected) return true
            return channels.contains(channelId)
        }

        fun startEdit(channels: List<Int>) {
            allChannels = HashSet(channels)
        }

        fun setChannel(channelId: Int, selected: Boolean) {
            if (selected) {
                channels.add(channelId)
            } else {
                if (this.selected) {
                    this.selected = false
                    this.channels.addAll(allChannels)
                }

                channels.remove(channelId)
            }
        }

        fun select() {
            this.selected = true
            this.channels.clear()
        }

        fun stopEdit() {
            if (channels.size == allChannels.size) {
                channels.clear()
                selected = true
            } else if (!selected && channels.isEmpty()) {
                site.setSensor(sensorId, false)
            }
        }

        fun copy(site: SelectorTreeSite): SelectorTreeSensor {
            val other = SelectorTreeSensor(site, sensorId)
            other.selected = this.selected
            other.channels.addAll(this.channels)
            return other
        }
    }

    fun clear() {
        sites.clear()
    }

    fun getSite(siteId: Int): SelectorTreeSite? {
        return sites[siteId]
    }

    fun getOrCreateSite(siteId: Int): SelectorTreeSite {
        var site = getSite(siteId)

        if (site == null) {
            site =
                SelectorTreeSite(
                    this,
                    siteId
                )
            sites[siteId] = site
        }

        return site
    }

    fun setSite(siteId: Int, selected: Boolean) {
        if (selected) {
            getOrCreateSite(siteId).select()
        } else {
            sites.remove(siteId)
        }
    }


    fun checkSite(siteId: Int): SelectionType {
        return sites[siteId]?.selection ?: SelectionType.UNSELECTED
    }

    fun checkSensor(siteId: Int, sensorId: Int): SelectionType {
        return sites[siteId]?.checkSensor(sensorId) ?: return SelectionType.UNSELECTED
    }

    fun checkChannel(siteId: Int, sensorId: Int, channelId: Int): Boolean {
        return sites[siteId]?.checkChannel(sensorId, channelId) ?: false
    }

    inline fun editSite(siteId: Int, sensors: List<Int>, action: (SelectorTreeSite) -> Unit) {
        val site = getOrCreateSite(siteId)
        site.startEdit(sensors)
        action(site)
        site.stopEdit()
    }

    fun summarizeSelected(): SummarizeData {
        val sites = ArrayList<Int>()
        val sensors = ArrayList<Int>()
        val channels = ArrayList<Int>()


        for (site in this.sites.values) {
            if (site.selected) {
                sites.add(site.siteId)
                continue
            }
            for (sensor in site.listSensors()) {
                if (sensor.selected) {
                    sensors.add(sensor.sensorId)
                    continue
                }
                channels.addAll(sensor.listChannels())
            }
        }

        return SummarizeData(sites, sensors, channels)
    }

    fun copy(): MusaSelectorTree {
        val other = MusaSelectorTree()
        for (site in this.sites.values) {
            other.sites[site.siteId] = site.copy(other)
        }
        return other
    }


    data class SummarizeData(val sites: List<Int>, val sensors: List<Int>,  val channels: List<Int>)
}
