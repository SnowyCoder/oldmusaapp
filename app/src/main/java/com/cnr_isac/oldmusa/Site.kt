package com.cnr_isac.oldmusa

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.cnr_isac.oldmusa.R.layout.*
import com.cnr_isac.oldmusa.api.ApiSensor
import com.cnr_isac.oldmusa.api.MapResizeData
import com.cnr_isac.oldmusa.api.Sensor
import com.cnr_isac.oldmusa.api.Site
import com.cnr_isac.oldmusa.util.ApiUtil.api
import com.cnr_isac.oldmusa.util.ApiUtil.isAdmin
import com.cnr_isac.oldmusa.util.ApiUtil.query
import com.cnr_isac.oldmusa.util.ApiUtil.useLoadingBar
import kotlinx.android.synthetic.main.add_sensor.*
import kotlinx.android.synthetic.main.edit_museum.*
import kotlinx.android.synthetic.main.remove_museum.*


class Site : Fragment(), SiteMapFragment.OnSensorSelectListener, SwipeRefreshLayout.OnRefreshListener {


    private lateinit var listView: ListView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    val args: SiteArgs by navArgs()

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

        val view = inflater.inflate(fragment_museum, container, false)

        // permission
        isAdmin {
            if (!it) return@isAdmin

            val buttonVisible1 = view.findViewById<ImageButton>(R.id.addMapbutton)
            buttonVisible1.visibility=View.VISIBLE

            val buttonVisible2 = view.findViewById<ImageButton>(R.id.addSensorbutton)
            buttonVisible2.visibility=View.VISIBLE

            /*val buttonVisible3 = findViewById<ImageButton>(R.id.addChannelButton)
            buttonVisible3.visibility=View.VISIBLE*/
        }

        listView = view.findViewById(R.id.SensorList)

        
        //view.bringToFront();

        // add event listener to array items
        listView.onItemClickListener = AdapterView.OnItemClickListener { _, view, index, _ ->
            val sensor = listView.adapter.getItem(index) as SensorData

            view.findNavController().navigate(SiteDirections.actionSiteToChannel(sensor.handle.id))
        }

        // open map options modal
        view.findViewById<ImageButton>(R.id.addMapbutton).setOnClickListener {
            val mBuilder = AlertDialog.Builder(context!!)
            mBuilder.setTitle("Aggiungi mappa")
            val d = mBuilder.setView(LayoutInflater.from(context!!).inflate(add_map, null)).create()
            val lp = WindowManager.LayoutParams()
            lp.copyFrom(d.window!!.attributes)
            lp.width = (resources.displayMetrics.widthPixels * 0.80).toInt()
            lp.height = (resources.displayMetrics.heightPixels * 0.35).toInt()
            d.show()
            d.window!!.attributes = lp

            /*d.AddButtonM.setOnClickListener { view ->
                d.dismiss()
            }*/

            val img_pick_btn = d.findViewById<Button>(R.id.img_pick_btn)


            img_pick_btn.setOnClickListener {
                //check runtime permission
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                    if (context!!.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) ==
                        PackageManager.PERMISSION_DENIED){
                        //permission denied
                        val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                        //show popup to request runtime permission
                        requestPermissions(permissions, PERMISSION_CODE)
                    }
                    else{
                        //permission already granted
                        pickImageFromGallery()
                    }
                }
                else{
                    //system OS is < Marshmallow
                    pickImageFromGallery()


                }

            }
        }

        // open add sensor modal
        view.findViewById<ImageButton>(R.id.addSensorbutton).setOnClickListener{
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
                val name = dialog.findViewById<EditText>(R.id.name)
                val idCnr = dialog.findViewById<EditText>(R.id.idCnr)
                val enabled = dialog.findViewById<CheckBox>(R.id.enabled)

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
        swipeRefreshLayout = view.findViewById(R.id.swipe_container) as SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener(this)

        swipeRefreshLayout.post {
            swipeRefreshLayout.isRefreshing = true

            reloadSite()
        }

        (childFragmentManager.findFragmentById(R.id.site_map)!! as SiteMapFragment).sensorSelectListener = this

        return view
    }

    override fun onRefresh() {
        reloadSite()
    }

    override fun onSensorSelect(sensorId: Long) {
        view!!.findNavController().navigate(SiteDirections.actionSiteToChannel(sensorId))
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        isAdmin {
            if (!it) return@isAdmin

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

                val nameSite = dialog.findViewById<EditText>(R.id.nameSite)
                val idcnrSite = dialog.findViewById<EditText>(R.id.IdCnrSite)

                nameSite.setText(currentSite.name ?: "")
                idcnrSite.setText(currentSite.idCnr ?: "")

                dialog.Aggiorna.setOnClickListener {
                    query {
                        currentSite.name = nameSite.text.toString()
                        currentSite.idCnr = idcnrSite.text.toString()
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
                    //(fragmentManager!!.findFragmentById(R.id.site_map) as SiteMapFragment).onRefresh(it, sensors)
                    //view!!.findViewById<SiteMapFragment>(R.id.site_map)
                    //
                    currentImageW = it.width
                    currentImageH = it.height
                    (childFragmentManager.findFragmentById(R.id.site_map)!! as SiteMapFragment).onRefresh(it, sensors)
                    view!!.findViewById<TextView>(R.id.noMapText).visibility = View.INVISIBLE
                    view!!.findViewById<ImageView>(R.id.noMapImage).visibility = View.INVISIBLE
                }
            } else {
                view!!.findViewById<TextView>(R.id.noMapText).visibility = View.VISIBLE
                view!!.findViewById<ImageView>(R.id.noMapImage).visibility = View.VISIBLE
            }

            val adapter = ArrayAdapter<SensorData>(context!!, list_sensor_item, list)
            listView.adapter = adapter
            activity?.title = currentSite.name ?: ""


            swipeRefreshLayout.isRefreshing = false
        }
    }


    private fun pickImageFromGallery() {
        //Intent to pick image
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, IMAGE_PICK_CODE)
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
