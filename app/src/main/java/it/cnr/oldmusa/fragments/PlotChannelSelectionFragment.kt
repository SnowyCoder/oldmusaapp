package it.cnr.oldmusa.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.collection.LruCache
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import it.cnr.oldmusa.*
import it.cnr.oldmusa.util.AndroidUtil.getBackStackEntry
import it.cnr.oldmusa.util.GraphQlUtil.query
import it.cnr.oldmusa.util.selection.CurrentSelectionModel
import it.cnr.oldmusa.util.selection.MusaSelectorTree.SelectorTreeSensor
import it.cnr.oldmusa.util.selection.MusaSelectorTree.SelectorTreeSite
import it.cnr.oldmusa.util.selection.SelectionCheckBox
import it.cnr.oldmusa.util.selection.SelectionType
import kotlinx.android.synthetic.main.fragment_plot_channel_selection.*
import kotlinx.android.synthetic.main.fragment_plot_channel_selection.view.*
import kotlinx.android.synthetic.main.fragment_plot_channel_selection_item.view.*


class PlotChannelSelectionFragment : Fragment(), SwipeRefreshLayout.OnRefreshListener {
    val args: PlotChannelSelectionFragmentArgs by navArgs()

    val finalSelectionModel: CurrentSelectionModel by viewModels({ getBackStackEntry(1)!! })
    val currentSelectionModel: CurrentSelectionModel by viewModels()

    var currentSite: SelectorTreeSite? = null
    var currentSensor: SelectorTreeSensor? = null

    val cache: LruCache<Pair<Int, Int>, Pair<String, List<ItemData>>> = LruCache(64)

    var isTreeInitialized = false

    private val backPressCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            navBack()
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_plot_channel_selection, container, false)

        isTreeInitialized = savedInstanceState?.getBoolean("isTreeInitialized") ?: false

        if (!isTreeInitialized) {
            isTreeInitialized = true
            currentSelectionModel.tree = finalSelectionModel.tree.copy()
        }

        // Set the layoutManager
        view.list.layoutManager = LinearLayoutManager(context)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity!!.onBackPressedDispatcher.addCallback(this, backPressCallback)
        backPressCallback.isEnabled = false

        save.setOnClickListener {
            finalSelectionModel.tree = currentSelectionModel.tree
            findNavController().popBackStack()
        }
        cancel.setOnClickListener {
            backPressCallback.isEnabled = false
            findNavController().popBackStack()
        }

        swipeContainer.setOnRefreshListener(this)

        navigateToArgs(args.defaultSensorId)
    }

    fun navigateToArgs(sensorId: Int) {
        swipeContainer.isRefreshing = true
        query(DescendToSensorListQuery(sensorId))
            .onResult { data ->
                backPressCallback.isEnabled = true

                // Cache sites
                cache.put(Pair(-1, -1), Pair("Sites", data.sites().map { ItemData(it.id(), it.name() ?: "null") }))

                // Start site edit
                val site = currentSelectionModel.tree.getOrCreateSite(data.sensor().site().id())
                currentSite = site
                site.startEdit(data.sensor().site().sensors().map { it.id() })

                // Cache sensors
                val siteId = data.sensor().site().id()
                val siteName = data.sites().find { it.id() == siteId }?.name() ?: "null"
                cache.put(Pair(siteId, -1), Pair(siteName, data.sensor().site().sensors().map { ItemData(it.id(), it.name() ?: "null") }))

                // Start edit sensor
                val sensor = site.getOrCreateSensor(sensorId)
                currentSensor = sensor
                sensor.startEdit(data.sensor().channels().map { it.id() })

                // Load the data
                loadData(
                    data.sensor().name() ?: "Channels",
                    data.sensor().channels().map { ItemData(it.id(), it.name() ?: "null") }
                )
            }.onDone { swipeContainer.isRefreshing = false }
    }

    fun loadCurrent() {
        val site = currentSite
        val sensor = currentSensor

        val cached = cache.get(Pair(site?.siteId ?: -1, sensor?.sensorId ?: -1))

        if (cached != null) {
            when {
                site == null -> {}
                sensor == null -> site.startEdit(cached.second.map { it.id })
                else -> sensor.startEdit(cached.second.map { it.id })
            }
            loadData(cached.first, cached.second)
            swipeContainer.isRefreshing = false
            return
        }

        swipeContainer.isRefreshing = true
        when {
            site == null -> {
                query(SiteListQuery())
                    .onResult { data ->
                        loadAndCacheData(
                            "Sites",
                            data.sites().map { ItemData(it.id(), it.name() ?: "null") }
                        )
                    }
            }
            sensor == null -> {
                query(SiteDetailsQuery(site.siteId))
                    .onResult { data ->
                        site.startEdit(data.site().sensors().map { it.id() })
                        loadAndCacheData(
                            data.site().name() ?: "Sensors",
                            data.site().sensors().map { ItemData(it.id(), it.name() ?: "null") }
                        )
                    }
            }
            else -> {
                query(SensorDetailsQuery(sensor.sensorId))
                    .onResult { data ->
                        sensor.startEdit(data.sensor().channels().map { it.id() })
                        loadAndCacheData(
                            data.sensor().name() ?: "Channels",
                            data.sensor().channels().map { ItemData(it.id(), it.name() ?: "null") }
                        )
                    }
            }
        }.onDone { swipeContainer.isRefreshing = false }
    }

    fun navBack(): Boolean {
        val site = currentSite
        val sensor = currentSensor

        return when {
            site == null -> {
                false
            }
            sensor == null -> {
                site.stopEdit()
                currentSite = null
                backPressCallback.isEnabled = false
                loadCurrent()
                true
            }
            else -> {
                sensor.stopEdit()
                currentSensor = null
                loadCurrent()
                true
            }
        }
    }

    private fun loadAndCacheData(title: String, data: List<ItemData>) {
        cache.put(Pair(currentSite?.siteId ?: -1, currentSensor?.sensorId ?: -1), Pair(title, data))
        loadData(title, data)
    }

    private fun loadData(title: String, data: List<ItemData>) {
        activity?.title = title
        list.adapter = MyRecyclerViewAdapter(data)
    }

    fun isChecked(id: Int): SelectionType {
        val site = currentSite
        val sensor = currentSensor
        return when {
            site == null -> currentSelectionModel.tree.checkSite(id)
            sensor == null -> site.checkSensor(id)
            else -> {
                if (sensor.checkChannel(id)) SelectionType.SELECTED
                else SelectionType.UNSELECTED
            }
        }
    }

    fun setChecked(id: Int, checked: Boolean) {
        val site = currentSite
        val sensor = currentSensor
        when {
            site == null -> currentSelectionModel.tree.setSite(id, checked)
            sensor == null -> site.setSensor(id, checked)
            else -> sensor.setChannel(id, checked)
        }
    }

    data class ItemData(val id: Int, val name: String)

    inner class MyRecyclerViewAdapter(
        val values: List<ItemData>
    ) : RecyclerView.Adapter<MyRecyclerViewAdapter.ViewHolder>() {
        private val mOnClickListener: View.OnClickListener

        init {
            mOnClickListener = View.OnClickListener { v ->
                val item = v.tag as ItemData
                val site = currentSite
                when {
                    site == null -> {
                        currentSite = currentSelectionModel.tree.getOrCreateSite(item.id)
                        backPressCallback.isEnabled = true
                        loadCurrent()
                    }
                    currentSensor == null -> {
                        currentSensor = site.getOrCreateSensor(item.id)
                        loadCurrent()
                    }
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.fragment_plot_channel_selection_item, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = values[position]
            holder.contentView.text = item.name
            holder.selectedView.setSelectionState(isChecked(item.id))

            with(holder.view) {
                tag = item
            }

            if (currentSensor == null) {
                holder.view.setOnClickListener(mOnClickListener)
                holder.selectedView.setOnCheckedChangeListener { _, checked ->
                    setChecked(item.id, checked)
                }
            } else {
                holder.view.setOnClickListener {
                    // SELECTED -> false
                    // PARTIAL -> false
                    // UNSELECTED -> true
                    val checked = holder.selectedView.selectionState == SelectionType.UNSELECTED
                    setChecked(item.id, checked)
                    holder.selectedView.setSelectionState(if (checked) SelectionType.SELECTED else SelectionType.UNSELECTED )
                }
                holder.selectedView.isClickable = false
            }
        }

        override fun getItemCount(): Int = values.size

        inner class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
            val contentView: TextView = view.content
            val selectedView: SelectionCheckBox = view.selected

            override fun toString(): String {
                return super.toString() + " '" + contentView.text + "'" + " '" + selectedView.selectionState + "'"
            }
        }
    }

    override fun onRefresh() {
        val cacheIndex = Pair(currentSite?.siteId ?: -1, currentSensor?.sensorId ?: -1)
        cache.remove(cacheIndex)
        loadCurrent()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("isTreeInitialized", isTreeInitialized)
    }

}
