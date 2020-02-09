package it.cnr.oldmusa.fragments


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import it.cnr.oldmusa.CollectChannelsQuery
import it.cnr.oldmusa.R
import it.cnr.oldmusa.util.AndroidUtil
import it.cnr.oldmusa.util.AndroidUtil.dateSelectPopupSetup
import it.cnr.oldmusa.util.AndroidUtil.getBackStackEntry
import it.cnr.oldmusa.util.AndroidUtil.useLoadingBar
import it.cnr.oldmusa.util.GraphQlUtil.query
import it.cnr.oldmusa.util.selection.CurrentSelectionModel
import kotlinx.android.synthetic.main.fragment_graph_options_builder.*
import java.util.*
import kotlin.collections.ArrayList

class GraphOptionsBuilder : Fragment() {
    val args: GraphOptionsBuilderArgs by navArgs()

    val selectionModel: CurrentSelectionModel by viewModels({ getBackStackEntry(0)!! })

    var treeInitialized = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_graph_options_builder, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (!treeInitialized) {
            treeInitialized = true
            selectionModel.tree.clear()
            // NOTE: unsafe operation, we should call startEdit/stopEdit but it's unnecessary here since
            // we know that the tree is cleared and we're adding an element, the tree couldn't be
            // optimized.
            selectionModel.tree
                .getOrCreateSite(args.siteId)
                .getOrCreateSensor(args.sensorId)
                .select()
        }

        val today = Calendar.getInstance()
        endDate.setText(AndroidUtil.isoSimpleDateFormat.format(today.time))
        today.add(Calendar.DAY_OF_MONTH, -1)
        startDate.setText(AndroidUtil.isoSimpleDateFormat.format(today.time))

        startDate.dateSelectPopupSetup()
        endDate.dateSelectPopupSetup()

        this.selectChannels.setOnClickListener {
            val dirs = GraphOptionsBuilderDirections.actionGraphOptionsBuilderToPlotChannelSelection(args.sensorId)
            findNavController().navigate(dirs)
        }

        this.draw.setOnClickListener {
            val startCal = Calendar.getInstance()
            val endCal = Calendar.getInstance()


            if (!AndroidUtil.parseIsoDate(startCal, this.startDate.text.toString()) ||
                !AndroidUtil.parseIsoDate(endCal, this.endDate.text.toString())) {
                Toast.makeText(context!!, "Invalid date!", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val selected = selectionModel.tree.summarizeSelected()

            if (selected.sites.isNotEmpty() || selected.sensors.isNotEmpty()) {
                query(CollectChannelsQuery(selected.sites, selected.sensors))
                    .onResult {
                        val channels = ArrayList(selected.channels)

                        it.sites().flatMapTo(channels) { site -> site.sensors().flatMap { sensor -> sensor.channels().map { ch -> ch.id() } } }
                        it.sensors().flatMapTo(channels) { sensor -> sensor.channels().map { ch -> ch.id() } }

                        navToPlot(startCal, endCal, channels)
                    }.useLoadingBar(this)
            } else {
                navToPlot(startCal, endCal, selected.channels)
            }
        }

        super.onViewCreated(view, savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("treeInitialized", treeInitialized)
    }


    private fun navToPlot(start: Calendar, end: Calendar, channels: List<Int>) {
        val dirs = GraphOptionsBuilderDirections.actionGraphOptionsBuilderToSensorGraph(
            channels.toIntArray(), start.timeInMillis, end.timeInMillis)
        findNavController().navigate(dirs)
    }
}
