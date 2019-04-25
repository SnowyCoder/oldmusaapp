package com.cnr_isac.oldmusa

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.util.Log.e
import android.view.*
import android.widget.*
import com.cnr_isac.oldmusa.Account.api
import com.cnr_isac.oldmusa.Account.isAdmin
import com.cnr_isac.oldmusa.R.layout.*
import com.cnr_isac.oldmusa.api.ApiSensor
import com.cnr_isac.oldmusa.api.Sensor
import com.cnr_isac.oldmusa.api.Site
import com.cnr_isac.oldmusa.util.ApiUtil.query
import com.cnr_isac.oldmusa.util.ApiUtil.withLoading
import com.cnr_isac.oldmusa.util.StreamUtil.getMd5
import com.squareup.timessquare.CalendarPickerView
import kotlinx.android.synthetic.main.activity_museum.*
import kotlinx.android.synthetic.main.add_sensor.*
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*


class Museum : AppCompatActivity() {

    lateinit var calendar: CalendarPickerView
        private set

    lateinit var mDrawerLayout: DrawerLayout
        private set

    lateinit var mToggle: ActionBarDrawerToggle
        private set

    private lateinit var site: Site


    data class SensorData(val handle: Sensor) {
        override fun toString(): String {
            return handle.name ?: handle.id.toString()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(activity_museum)

        // get site

        // permission
        isAdmin {
            if (!it) return@isAdmin

            val buttonVisible1 = findViewById<ImageButton>(R.id.addMapbutton)
            buttonVisible1.visibility=View.VISIBLE

            val buttonVisible2 = findViewById<ImageButton>(R.id.addSensorbutton)
            buttonVisible2.visibility=View.VISIBLE

            val buttonVisible3 = findViewById<ImageButton>(R.id.addChannelButton)
            buttonVisible3.visibility=View.VISIBLE
        }

        val listView = findViewById<ListView>(R.id.SensorList)

        
        //view.bringToFront();

        // get site
        query {
            // Query everything needed in async
            site = api.getSite(intent.getLongExtra("site", -1))
            Pair(site.sensors, site.getMap())
        }.onResult { (sensors, mapData) ->
            // Then use it in the sync thread
            val list = sensors.map { SensorData(it) }

            val map = findViewById<ImageView>(R.id.mapMuseum)

            val data = mapData?.readBytes()

            data?.let {
                findViewById<TextView>(R.id.noMapText).visibility = View.INVISIBLE
                findViewById<ImageView>(R.id.noMapImage).visibility = View.INVISIBLE
                map.setImageBitmap(BitmapFactory.decodeByteArray(data, 0, data.size))
            }

            val adapter = ArrayAdapter<SensorData>(this, list_sensor_item, list)
            listView.adapter = adapter
        }.withLoading(this)

        // add event listener to array items
        listView.onItemClickListener = AdapterView.OnItemClickListener { _, view, index, _ ->
            val sensor = listView.adapter.getItem(index) as SensorData

            val intent = Intent(this, QuickGraph::class.java)
            intent.putExtra("sensor", sensor.handle.id)
            startActivity(intent)

            /*val mBuilder = AlertDialog.Builder(this)
            mBuilder.setTitle("Crea grafico")
            val d = mBuilder.setView(LayoutInflater.from(this).inflate(create_graph, null)).create()
            val lp = WindowManager.LayoutParams()
            lp.copyFrom(d.window!!.attributes)
            lp.title = "Crea grafico"
            lp.width = (resources.displayMetrics.widthPixels * 0.90).toInt()
            lp.height = (resources.displayMetrics.heightPixels * 0.80).toInt()
            d.show()
            d.window!!.attributes = lp

            // set up calendar
            val nextYear = Calendar.getInstance()
            nextYear.add(Calendar.YEAR, 1)

            calendar = d.findViewById<CalendarPickerView>(R.id.calendar_view)
            val today = Date()
            calendar.init(today, nextYear.time)
                .withSelectedDate(today)
                .inMode(CalendarPickerView.SelectionMode.RANGE)
            calendar.highlightDates(getHolidays())

            // submit graph button listener
            d.create.setOnClickListener { view -> // create = bottone invio
                d.dismiss()
            }*/
        }

        // open map options modal
        addMapbutton.setOnClickListener {
            val mBuilder = AlertDialog.Builder(this)
            mBuilder.setTitle("Aggiungi mappa")
            val d = mBuilder.setView(LayoutInflater.from(this).inflate(add_map, null)).create()
            val lp = WindowManager.LayoutParams()
            lp.copyFrom(d.window!!.attributes)
            lp.width = (resources.displayMetrics.widthPixels * 0.80).toInt()
            lp.height = (resources.displayMetrics.heightPixels * 0.40).toInt()
            d.show()
            d.window!!.attributes = lp

            /*d.AddButtonM.setOnClickListener { view ->
                d.dismiss()
            }*/

            val img_pick_btn = d.findViewById<Button>(R.id.img_pick_btn)


            img_pick_btn.setOnClickListener {
                //check runtime permission
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                    if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) ==
                        PackageManager.PERMISSION_DENIED){
                        //permission denied
                        val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                        //show popup to request runtime permission
                        requestPermissions(permissions, Museum.PERMISSION_CODE)
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

        // setup drawer layout for modals
        mDrawerLayout = findViewById(R.id.drawerSite)
        mToggle = ActionBarDrawerToggle(this, mDrawerLayout, R.string.open, R.string.close)
        mDrawerLayout.addDrawerListener(mToggle)
        mToggle.syncState()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // open add sensor modal
        addSensorbutton.setOnClickListener{
            //val mDialogView = LayoutInflater.from(this).inflate(R.layout.add_museum, null)

            val mBuilder = AlertDialog.Builder(this)
            mBuilder.setTitle("Aggiungi sensore")
            val d = mBuilder.setView(LayoutInflater.from(this).inflate(add_sensor, null)).create()
            val lp = WindowManager.LayoutParams()
            lp.copyFrom(d.window!!.attributes)
            lp.title = "Aggiungi sensore"
            lp.width = (resources.displayMetrics.widthPixels * 0.80).toInt()
            lp.height = (resources.displayMetrics.heightPixels * 0.60).toInt()
            d.show()
            d.window!!.attributes = lp

            d.AddButtonS.setOnClickListener {
                val nameSensor = d.findViewById<EditText>(R.id.nameSensore)
                e("print", nameSensor.toString())
                //val mappaSensor = view.findViewById<EditText>(R.id.mappaSensore)
                val idcnrSensor = d.findViewById<EditText>(R.id.idcnrSensore)
                e("print", idcnrSensor.toString())

                query {
                    site.addSensor(
                        ApiSensor(
                            name = nameSensor.text.toString(),
                            idCnr = idcnrSensor.text.toString().toLong()
                        )
                    )
                }.onResult {
                    d.dismiss()
                    val refresh = Intent(this, Museum::class.java)
                    refresh.putExtra("site", site.id)
                    startActivity(refresh)
                }
            }
        }

    }

    @SuppressLint("ResourceType")
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.layout.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        /*when (id) {
            R.id.action_settings -> return true
            R.id.action_next -> {
                val selectedDates = calendar
                    .selectedDates as ArrayList<Date>
                Toast.makeText(
                    this@Museum, selectedDates.toString(),
                    Toast.LENGTH_LONG
                ).show()
                return true
            }
        }*/
        return super.onOptionsItemSelected(item)
    }

    private fun getHolidays(): ArrayList<Date> {
        val sdf = SimpleDateFormat("dd-M-yyyy")
        val dateInString = "21-04-2015"
        var date: Date? = null
        try {
            date = sdf.parse(dateInString)
        } catch (e: ParseException) {
            e.printStackTrace()
        }

        val holidays = ArrayList<Date>()
        if (date != null) {
            holidays.add(date)
        }
        return holidays
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
                if (grantResults.size >0 && grantResults[0] ==
                    PackageManager.PERMISSION_GRANTED){
                    //permission from popup granted
                    pickImageFromGallery()
                }
                else{
                    //permission from popup denied
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    //handle result of picked image
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (data == null) return

        if (resultCode == Activity.RESULT_OK && requestCode == IMAGE_PICK_CODE){
            val bytes = contentResolver.openInputStream(data.data!!)!!.readBytes()

            Log.i(TAG, "Loading image: ${bytes.getMd5()}")

            site.setMap(contentResolver.openInputStream(data.data!!)!!)

            val refresh = Intent(this, Museum::class.java)
            refresh.putExtra("site", site.id)
            startActivity(refresh)
            finish()
        }
    }

    companion object {
        //image pick code
        private val IMAGE_PICK_CODE = 1000
        //Permission code
        private val PERMISSION_CODE = 1001
        private const val TAG = "Museum"
    }
}
