package com.cnr_isac.oldmusa.fragments

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.cnr_isac.oldmusa.Account.isAdmin
import com.cnr_isac.oldmusa.R
import com.cnr_isac.oldmusa.R.layout.*
import com.cnr_isac.oldmusa.api.ApiSensor
import com.cnr_isac.oldmusa.api.MapResizeData
import com.cnr_isac.oldmusa.api.Sensor
import com.cnr_isac.oldmusa.api.Site
import com.cnr_isac.oldmusa.util.ApiUtil.api
import com.cnr_isac.oldmusa.util.ApiUtil.query
import kotlinx.android.synthetic.main.add_map.*
import kotlinx.android.synthetic.main.add_sensor.*
import kotlinx.android.synthetic.main.edit_museum.*
import kotlinx.android.synthetic.main.fragment_home.swipeContainer
import kotlinx.android.synthetic.main.fragment_site.*
import kotlinx.android.synthetic.main.remove_museum.*


class SiteFragment : Fragment(),
    SiteMapFragment.OnSensorSelectListener, SwipeRefreshLayout.OnRefreshListener {


    val args: SiteFragmentArgs by navArgs()

    lateinit var currentSite: Site

    var currentImageW: Int = 0
    var currentImageH: Int = 0

    data class SensorData(val handle: Sensor) {
        override fun toString(): String {
            return handle.name ?: handle.id.toString()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        setHasOptionsMenu(true)
        activity?.title = "Sito"

        val view = inflater.inflate(fragment_site, container, false)


        (childFragmentManager.findFragmentById(R.id.siteMap)!! as SiteMapFragment).sensorSelectListener = this

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // permission
        if (isAdmin) {
            addMapbutton.visibility = View.VISIBLE

            addSensorbutton.visibility = View.VISIBLE
        }

        // add event listener to array items
        sensorList.onItemClickListener = AdapterView.OnItemClickListener { _, view, index, _ ->
            val sensor = sensorList.adapter.getItem(index) as SensorData

            view.findNavController().navigate(
                SiteFragmentDirections.actionSiteToChannel(
                    sensor.handle.id
                )
            )
        }

        // open map options modal
        addMapbutton.setOnClickListener {
            val mBuilder = AlertDialog.Builder(context!!)
            mBuilder.setTitle("Aggiungi mappa")
            val d = mBuilder.setView(LayoutInflater.from(context!!).inflate(add_map, null)).create()
            val lp = WindowManager.LayoutParams()
            lp.copyFrom(d.window!!.attributes)
            lp.width = (resources.displayMetrics.widthPixels * 0.80).toInt()
            lp.height = (resources.displayMetrics.heightPixels * 0.35).toInt()
            d.show()
            d.window!!.attributes = lp


            d.imgPickButton.setOnClickListener {
                // check runtime permission
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                    if (context!!.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) ==
                        PackageManager.PERMISSION_DENIED){
                        // permission denied
                        val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                        // show popup to request runtime permission
                        requestPermissions(permissions,
                            PERMISSION_CODE
                        )
                    }
                    else{
                        // permission already granted
                        pickImageFromGallery()
                    }
                }
                else{
                    // system OS is < Marshmallow
                    pickImageFromGallery()
                }

            }
        }

        // open add sensor modal
        addSensorbutton.setOnClickListener {
            //val mDialogView = LayoutInflater.from(this).inflate(R.layout.add_museum, null)

            val mBuilder = AlertDialog.Builder(context!!)
            mBuilder.setTitle("Aggiungi sensore")
            val dialog = mBuilder.setView(LayoutInflater.from(context!!).inflate(add_sensor, null)).create()
            val lp = WindowManager.LayoutParams()
            lp.copyFrom(dialog.window!!.attributes)
            lp.title = "Aggiungi sensore"
            lp.width = (resources.displayMetrics.widthPixels * 0.80).toInt()
            lp.height = (resources.displayMetrics.heightPixels * 0.55).toInt()
            dialog.show()
            dialog.window!!.attributes = lp

            dialog.AddButtonS.setOnClickListener {
                val name = dialog.name
                val idCnr = dialog.idCnr
                val enabled = dialog.enabled

                query {
                    currentSite.addSensor(
                        ApiSensor(
                            name = name.text.toString(),
                            idCnr = idCnr.text.toString(),
                            locX = currentImageW.toLong() / 2,
                            locY = currentImageH.toLong() / 2,
                            enabled = enabled.isChecked
                        )
                    )
                }.onResult {
                    dialog.dismiss()
                    reloadSite()
                }
            }
        }

        // SwipeRefreshLayout
        swipeContainer.setOnRefreshListener(this)

        swipeContainer.post {
            swipeContainer.isRefreshing = true

            reloadSite()
        }
    }

    override fun onRefresh() {
        reloadSite()
    }

    override fun onSensorSelect(sensorId: Long) {
        view!!.findNavController().navigate(
            SiteFragmentDirections.actionSiteToChannel(
                sensorId
            )
        )
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (isAdmin) {
            inflater.inflate(R.menu.overflow_menu, menu)
            super.onCreateOptionsMenu(menu, inflater)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.remove -> {
                val mBuilder = AlertDialog.Builder(context!!)
                val dialog = mBuilder.setView(LayoutInflater.from(context!!).inflate(remove_museum, null)).create()
                val lp = WindowManager.LayoutParams()
                lp.copyFrom(dialog.window!!.attributes)
                lp.width = (resources.displayMetrics.widthPixels * 0.75).toInt()
                lp.height = (resources.displayMetrics.heightPixels * 0.30).toInt()
                dialog.show()
                dialog.window!!.attributes = lp

                dialog.ButtonYes.setOnClickListener {
                    query {
                        currentSite.delete()
                    }.onResult {
                        dialog.dismiss()
                        this.activity!!.onBackPressed()
                    }
                }
                dialog.ButtonNo.setOnClickListener {
                    dialog.dismiss()
                }

            }
            R.id.edit -> {
                val mBuilder = AlertDialog.Builder(context!!)
                mBuilder.setTitle("Modifica il museo")
                val dialog = mBuilder.setView(LayoutInflater.from(context!!).inflate(edit_museum, null)).create()
                val lp = WindowManager.LayoutParams()
                lp.copyFrom(dialog.window!!.attributes)
                lp.title = "modifica il museo"
                lp.width = (resources.displayMetrics.widthPixels * 0.80).toInt()
                lp.height = (resources.displayMetrics.heightPixels * 0.50).toInt()
                dialog.show()
                dialog.window!!.attributes = lp

                val nameSite = dialog.nameSite
                val idCnrSite = dialog.idCnrSite

                nameSite.setText(currentSite.name ?: "")
                idCnrSite.setText(currentSite.idCnr ?: "")

                dialog.Aggiorna.setOnClickListener {
                    query {
                        currentSite.name = nameSite.text.toString()
                        currentSite.idCnr = idCnrSite.text.toString()
                        currentSite.commit()
                    }.onResult {
                        dialog.dismiss()
                        reloadSite()
                    }
                }

            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun reloadSite() {
        val siteId = args.siteId

        query {
            // Query everything needed in async
            currentSite = api.getSite(siteId)

            val sensors = currentSite.sensors

            val mapData = currentSite.getMap()
                ?.readBytes()
                ?.let {
                BitmapFactory.decodeByteArray(it, 0, it.size)
            }

            Pair(sensors, mapData)
        }.onResult { (sensors, mapData) ->
            // Then use it in the sync thread
            val list = sensors.map { SensorData(it) }
            //view!!.findViewById<TextView>(R.id.noMapText).visibility = View.INVISIBLE
            //view!!.findViewById<ImageView>(R.id.noMapImage).visibility = View.INVISIBLE
            if (mapData != null) {
                mapData.let {
                    currentImageW = it.width
                    currentImageH = it.height
                    (childFragmentManager.findFragmentById(R.id.siteMap)!! as SiteMapFragment).onRefresh(it, sensors)
                    noMapText.visibility = View.INVISIBLE
                    noMapImage.visibility = View.INVISIBLE
                }
            } else {
                noMapText.visibility = View.VISIBLE
                noMapImage.visibility = View.VISIBLE
            }

            val adapter = ArrayAdapter<SensorData>(context!!, list_sensor_item, list)
            sensorList.adapter = adapter
            noSensorText.visibility = if (sensors.isEmpty()) View.VISIBLE else View.GONE
            activity?.title = currentSite.name ?: ""


            swipeContainer.isRefreshing = false
        }
    }


    private fun pickImageFromGallery() {
        //Intent to pick image
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent,
            IMAGE_PICK_CODE
        )
    }

    //handle requested permission result
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when(requestCode){
            PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    //permission from popup granted
                    pickImageFromGallery()
                }
                else{
                    //permission from popup denied
                    Toast.makeText(context!!, "Permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    //handle result of picked image
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (data == null) return

        if (resultCode == Activity.RESULT_OK && requestCode == IMAGE_PICK_CODE){
            val bytes = context!!.contentResolver.openInputStream(data.data!!)!!.readBytes()

            var resize: MapResizeData? = null

            if (currentImageW != 0) {
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                resize = MapResizeData(currentImageW, currentImageH, bitmap.width, bitmap.height)
            }

            currentSite.setMap(context!!.contentResolver.openInputStream(data.data!!)!!, resize)

            reloadSite()
        }
    }

    companion object {
        //image pick code
        private val IMAGE_PICK_CODE = 1000
        //Permission code
        private val PERMISSION_CODE = 1001
        private const val TAG = "Site"
    }
}
