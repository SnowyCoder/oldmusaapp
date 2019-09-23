package com.cnr_isac.oldmusa.fragments

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
import com.cnr_isac.oldmusa.Account.isAdmin
import com.cnr_isac.oldmusa.R
import com.cnr_isac.oldmusa.api.ApiChannel
import com.cnr_isac.oldmusa.api.Channel
import com.cnr_isac.oldmusa.api.Sensor
import com.cnr_isac.oldmusa.util.ApiUtil.api
import com.cnr_isac.oldmusa.util.ApiUtil.query
import com.cnr_isac.oldmusa.util.ApiUtil.useLoadingBar
import kotlinx.android.synthetic.main.add_channel.*
import kotlinx.android.synthetic.main.edit_sensor.*
import kotlinx.android.synthetic.main.fragment_sensor.*
import kotlinx.android.synthetic.main.remove_sensor.*

class SensorFragment : Fragment(), SwipeRefreshLayout.OnRefreshListener{

    lateinit var channels: List<Channel>

    val args: SensorFragmentArgs by navArgs()

    lateinit var currentSensor: Sensor


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


                query {
                    currentSensor.addChannel(
                        ApiChannel(
                            name = nameChannel.text.toString(),
                            idCnr = idCnrChannel.text.toString(),
                            measureUnit = measureUnit.text.toString(),
                            rangeMin = rangeMin,
                            rangeMax = rangeMax
                        )
                    )
                }.onResult {
                    dialog.dismiss()
                    reload()
                }
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

    fun onChannelSelect(channelId: Long) {
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
                    query {
                        currentSensor.delete()
                    }.onResult {
                        dialog.dismiss()
                        activity!!.onBackPressed()
                    }
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

                //Log.e(nameSen.toString(),idcnrSen.toString())

                name.setText(currentSensor.name ?: "")
                idCnr.setText(currentSensor.idCnr ?: "")
                enabled.isChecked = currentSensor.enabled

                dialog.aggiorna.setOnClickListener {
                    query {
                        currentSensor.name = name.text.toString()
                        currentSensor.idCnr = idCnr.text.toString()
                        currentSensor.enabled = enabled.isChecked
                        currentSensor.commit()
                    }.onResult {
                        dialog.dismiss()
                        reload()
                    }
                }
            }
            R.id.resetPosition -> {
                // This will be slow as hell
                query {
                    val mapData = api.getSiteMap(currentSensor.siteId)?.readBytes()
                    val map = mapData?.let { BitmapFactory.decodeByteArray(mapData, 0, mapData.size) }

                    val x = map?.width ?: 0
                    val y = map?.height ?: 0

                    currentSensor.locX = x.toLong() / 2
                    currentSensor.locY = y.toLong() / 2
                    currentSensor.commit()
                }.useLoadingBar(this)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun reload() {

        //channelId è invece l'id del sensore
        val sensorId = args.channelId
        // permission
        /*isAdmin {
            if (!it) return@isAdmin

            val buttonVisible = view.findViewById<ImageButton>(R.id.addSiti)
            buttonVisible.visibility = View.VISIBLE
        }*/


        query {
            currentSensor = api.getSensor(sensorId)
            currentSensor.channels
        }.onResult { listChannels ->
            activity?.title = currentSensor.name ?: "Sensore"
            this.channels = listChannels

            val nameList = listChannels.map { it.name ?: "null" }

            Log.e(HomeFragment.TAG, nameList.toString())

            val adapter = ArrayAdapter<String>(view!!.context,
                R.layout.list_channel_item, nameList)
            channelList.adapter = adapter

            swipeContainer.isRefreshing = false
            noChannelText.visibility = if (listChannels.isEmpty()) View.VISIBLE else View.GONE
        }

        channelList.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val action = SensorFragmentDirections.actionSensorToQuickGraph(
                channels[position].id
            )
            view!!.findNavController().navigate(action)
        }

    }

}