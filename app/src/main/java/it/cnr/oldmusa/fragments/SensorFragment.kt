package it.cnr.oldmusa.fragments

import android.app.AlertDialog
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import it.cnr.oldmusa.*
import it.cnr.oldmusa.Account.isAdmin
import it.cnr.oldmusa.type.ChannelInput
import it.cnr.oldmusa.type.SensorInput
import it.cnr.oldmusa.util.GraphQlUtil.query
import it.cnr.oldmusa.util.GraphQlUtil.mutate
import it.cnr.oldmusa.util.AndroidUtil.useLoadingBar
import it.cnr.oldmusa.util.AndroidUtil.toNullableString
import it.cnr.oldmusa.util.GraphQlUtil.downloadImage
import kotlinx.android.synthetic.main.add_channel.*
import kotlinx.android.synthetic.main.edit_sensor.*
import kotlinx.android.synthetic.main.fragment_sensor.*
import kotlinx.android.synthetic.main.fragment_sensor.swipeContainer
import kotlinx.android.synthetic.main.fragment_site.*
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
            val builder = AlertDialog.Builder(context!!)
            builder.setTitle("Aggiungi canale")
            val dialogView = LayoutInflater.from(context!!).inflate(R.layout.add_channel, null)
            val dialog = builder.setView(dialogView).create()
            val lp = WindowManager.LayoutParams()
            lp.copyFrom(dialog.window!!.attributes)
            lp.title = "Aggiungi canale"
            lp.width = (resources.displayMetrics.widthPixels * 0.90).toInt()
            lp.height = (resources.displayMetrics.heightPixels * 0.70).toInt()
            dialog.show()
            dialog.window!!.attributes = lp

            dialog.addButton.setOnClickListener {
                val nameChannel = dialog.nameChannel
                val idCnrChannel = dialog.findViewById<EditText>(R.id.idCnr)
                val measureUnit = dialog.measureUnit
                val minRangeChannel = dialog.rangeMin
                val maxRangeChannel = dialog.rangeMax

                val rawRangeMin = minRangeChannel.text.toString()
                val rawRangeMax = maxRangeChannel.text.toString()

                val rangeMin = rawRangeMin.toDoubleOrNull()
                val rangeMax = rawRangeMax.toDoubleOrNull()

                mutate(AddChannelMutation(
                    args.sensorId,
                    ChannelInput.builder()
                        .name(nameChannel.text.toNullableString())
                        .idCnr(idCnrChannel.text.toNullableString())
                        .measureUnit(measureUnit.text.toNullableString())
                        .rangeMin(rangeMin)
                        .rangeMax(rangeMax)
                        .build()
                )).onResult {
                    dialog.dismiss()
                    reload()
                }.useLoadingBar(this)
            }
        }

        // SwipeRefreshLayout
        swipeContainer.setOnRefreshListener(this)

        swipeContainer.post {
            swipeContainer.isRefreshing = true
            reload()
        }
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
            super.onCreateOptionsMenu(menu, inflater)
        }
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
                val mBuilder = AlertDialog.Builder(context!!)
                mBuilder.setTitle("Modifica il sensore")
                val dialog = mBuilder.setView(LayoutInflater.from(context!!).inflate(R.layout.edit_sensor, null)).create()
                val lp = WindowManager.LayoutParams()
                lp.copyFrom(dialog.window!!.attributes)
                lp.title = "modifica il sensore"
                lp.width = (resources.displayMetrics.widthPixels * 0.80).toInt()
                lp.height = (resources.displayMetrics.heightPixels * 0.50).toInt()
                dialog.show()
                dialog.window!!.attributes = lp


                val name = dialog.name
                val idCnr = dialog.findViewById<EditText>(R.id.idCnr)
                val enabled = dialog.enabled

                name.setText(currentSensor.name() ?: "")
                idCnr.setText(currentSensor.idCnr() ?: "")
                enabled.isChecked = currentSensor.enabled()

                dialog.aggiorna.setOnClickListener {
                    mutate(UpdateSensorMutation(
                        args.sensorId,
                        SensorInput.builder()
                            .name(name.text.toNullableString())
                            .idCnr(idCnr.text.toNullableString())
                            .enabled(enabled.isChecked)
                            .build()
                    )).onResult {
                        dialog.dismiss()
                        reload()
                    }.useLoadingBar(this)
                }
            }
            R.id.resetPosition -> {
                // TODO
                // This will be slow as hell
                downloadImage(requireContext(), currentSensor.siteId()).onResult {
                    val x = it?.width ?: 0
                    val y = it?.height ?: 0

                    mutate(UpdateSensorMutation(
                        args.sensorId,
                        SensorInput.builder()
                            .locX(x / 2)
                            .locY(y / 2)
                            .build()
                    )).useLoadingBar(this)
                }.useLoadingBar(this)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun reload() {
        // permission
        /*isAdmin {
            if (!it) return@isAdmin

            val buttonVisible = view.findViewById<ImageButton>(R.id.addSiti)
            buttonVisible.visibility = View.VISIBLE
        }*/

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