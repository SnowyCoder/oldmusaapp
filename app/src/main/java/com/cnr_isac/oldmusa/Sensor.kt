package com.cnr_isac.oldmusa

import android.app.AlertDialog
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import com.cnr_isac.oldmusa.api.ApiChannel
import com.cnr_isac.oldmusa.api.Channel
import com.cnr_isac.oldmusa.api.Sensor
import com.cnr_isac.oldmusa.util.ApiUtil.api
import com.cnr_isac.oldmusa.util.ApiUtil.isAdmin
import com.cnr_isac.oldmusa.util.ApiUtil.query
import com.cnr_isac.oldmusa.util.ApiUtil.useLoadingBar
import kotlinx.android.synthetic.main.add_channel.*
import kotlinx.android.synthetic.main.edit_channel.*
import kotlinx.android.synthetic.main.edit_sensor.*
import kotlinx.android.synthetic.main.remove_sensor.*

class Sensor : Fragment(){
    lateinit var listChannels: List<Channel>
    private lateinit var listView: ListView

    val args: SensorArgs by navArgs()

    lateinit var currentSensor: Sensor


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        setHasOptionsMenu(true)

        val view = inflater.inflate(R.layout.fragment_sensor, container, false)

        listView = view.findViewById(R.id.channelsList)

        activity?.title = "Sensore"

        isAdmin {
            if (!it) return@isAdmin

            val buttonVisible1 = view.findViewById<ImageButton>(R.id.addChannelButton)
            buttonVisible1.visibility = View.VISIBLE
        }


        // open add channel modal
        view.findViewById<ImageButton>(R.id.addChannelButton).setOnClickListener{
            //val mDialogView = LayoutInflater.from(this).inflate(R.layout.add_museum, null)

            val mBuilder = AlertDialog.Builder(context!!)
            mBuilder.setTitle("Aggiungi canale")
            val dialogView = LayoutInflater.from(context!!).inflate(R.layout.add_channel, null)
            val dialog = mBuilder.setView(dialogView).create()
            val lp = WindowManager.LayoutParams()
            lp.copyFrom(dialog.window!!.attributes)
            lp.title = "Aggiungi canale"
            lp.width = (resources.displayMetrics.widthPixels * 0.90).toInt()
            lp.height = (resources.displayMetrics.heightPixels * 0.70).toInt()
            dialog.show()
            dialog.window!!.attributes = lp

            dialog.AddButtonC.setOnClickListener {
                val nameChannel = dialog.findViewById<EditText>(R.id.nameChannel)
                val idCnrChannel = dialog.findViewById<EditText>(R.id.idCnr)
                val measureUnit = dialog.findViewById<EditText>(R.id.measureUnit)
                val minRangeChannel = dialog.findViewById<EditText>(R.id.rangeMin)
                val maxRangeChannel = dialog.findViewById<EditText>(R.id.rangeMax)

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
                    reload(view)
                }
            }
        }
        reload(view)

        return view
    }

    fun onChannelSelect(channelId: Long) {
        view!!.findNavController().navigate(SensorDirections.actionSensorToQuickGraph(channelId))
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        isAdmin {
            if (!it) return@isAdmin

            inflater.inflate(R.menu.sensor_overflow_menu, menu)
            super.onCreateOptionsMenu(menu, inflater)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.remove -> {
                val mBuilder = AlertDialog.Builder(context!!)
                val dialog = mBuilder.setView(LayoutInflater.from(context!!).inflate(R.layout.remove_sensor, null)).create()
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


                val name = dialog.findViewById<EditText>(R.id.name)
                val idCnr = dialog.findViewById<EditText>(R.id.idCnr)
                val enabled = dialog.findViewById<CheckBox>(R.id.enabled)

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
                        //reload(View)
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

    fun reload(view: View) {

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
            this.listChannels = listChannels

            val nameList = listChannels.map { it.name ?: "null" }

            Log.e(Home.TAG, nameList.toString())

            val adapter = ArrayAdapter<String>(view.context, R.layout.list_channel_item, nameList)
            listView.adapter = adapter
        }.useLoadingBar(this)

        listView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val action = SensorDirections.actionSensorToQuickGraph(listChannels[position].id)
            view.findNavController().navigate(action)
        }

    }

}