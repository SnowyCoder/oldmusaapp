package it.cnr.oldmusa.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import it.cnr.oldmusa.Account.isAdmin
import it.cnr.oldmusa.DeleteSensorMutation
import it.cnr.oldmusa.R
import it.cnr.oldmusa.SensorDetailsQuery
import it.cnr.oldmusa.UpdateSensorMutation
import it.cnr.oldmusa.type.SensorUpdateInput
import it.cnr.oldmusa.util.AndroidUtil.linkToList
import it.cnr.oldmusa.util.AndroidUtil.useLoadingBar
import it.cnr.oldmusa.util.AsyncUtil.async
import it.cnr.oldmusa.util.GraphQlUtil.downloadImageSync
import it.cnr.oldmusa.util.GraphQlUtil.mutate
import it.cnr.oldmusa.util.GraphQlUtil.query
import kotlinx.android.synthetic.main.fragment_sensor.*
import kotlinx.android.synthetic.main.remove_sensor.*

class SensorFragment : Fragment(), SwipeRefreshLayout.OnRefreshListener{

    lateinit var channels: List<SensorDetailsQuery.Channel>

    val args: SensorFragmentArgs by navArgs()

    lateinit var currentSensor: SensorDetailsQuery.Sensor


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        setHasOptionsMenu(true)
        activity?.title = "Sensore"

        return inflater.inflate(R.layout.fragment_sensor, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (isAdmin) {
            addChannelButton.visibility = View.VISIBLE
        }

        // open add channel modal
        addChannelButton.setOnClickListener {
            findNavController().navigate(SensorFragmentDirections.actionSensorToCreateChannelFragment(args.sensorId))
        }

        // SwipeRefreshLayout
        swipeContainer.setOnRefreshListener(this)

        swipeContainer.post {
            swipeContainer.isRefreshing = true
            reload()
        }

        swipeContainer.linkToList(channelList)
    }

    override fun onRefresh() {
        reload()
    }

    fun onChannelSelect(channelId: Int) {
        view!!.findNavController().navigate(
            SensorFragmentDirections.actionSensorToQuickGraph(
                channelId
            )
        )
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (isAdmin) {
            inflater.inflate(R.menu.sensor_overflow_menu, menu)
        }

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.remove -> {
                val builder = AlertDialog.Builder(context!!)
                val dialog = builder.setView(LayoutInflater.from(context!!).inflate(R.layout.remove_sensor, null)).create()
                val lp = WindowManager.LayoutParams()
                lp.copyFrom(dialog.window!!.attributes)
                lp.width = (resources.displayMetrics.widthPixels * 0.75).toInt()
                lp.height = (resources.displayMetrics.heightPixels * 0.30).toInt()
                dialog.show()
                dialog.window!!.attributes = lp

                dialog.ButtonYesSensor.setOnClickListener {
                    mutate(DeleteSensorMutation(args.sensorId)).onResult {
                        dialog.dismiss()
                        activity!!.onBackPressed()
                    }.useLoadingBar(this)
                }
                dialog.ButtonNoSensor.setOnClickListener {
                    dialog.dismiss()
                }
            }
            R.id.edit -> {
                findNavController().navigate(
                    SensorFragmentDirections.actionSensorToCreateSensor(
                        currentSensor.siteId(),
                        CreateSensorFragment.SensorDetails(
                            args.sensorId,
                            currentSensor.name(),
                            currentSensor.idCnr(),
                            currentSensor.enabled()
                        )
                    )
                )
            }
            R.id.resetPosition -> {
                // This will be slow as hell
                async {
                    downloadImageSync(requireContext(), currentSensor.siteId())
                }.onResult {
                    val x = it?.width ?: 0
                    val y = it?.height ?: 0

                    mutate(UpdateSensorMutation(
                        args.sensorId,
                        SensorUpdateInput.builder()
                            .locX(x / 2)
                            .locY(y / 2)
                            .build()
                    )).useLoadingBar(this)
                }.useLoadingBar(this)
            }
            R.id.complexGraph -> {
                findNavController().navigate(
                    SensorFragmentDirections.actionSensorToGraphOptionsBuilder(args.sensorId)
                )
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun reload() {
        query(SensorDetailsQuery(args.sensorId)).onResult { data ->
            currentSensor = data.sensor()
            activity?.title = currentSensor.name() ?: "Sensore"
            this.channels = currentSensor.channels()

            val nameList = channels.map { it.name() ?: "null" }

            val adapter = ArrayAdapter<String>(view!!.context,
                R.layout.list_channel_item, nameList)
            channelList.adapter = adapter

            swipeContainer.isRefreshing = false
            noChannelText.visibility = if (channels.isEmpty()) View.VISIBLE else View.GONE
        }

        channelList.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val action = SensorFragmentDirections.actionSensorToQuickGraph(
                channels[position].id()
            )
            view!!.findNavController().navigate(action)
        }

    }

}