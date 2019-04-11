package com.cnr_isac.oldmusa

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.drm.DrmStore
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.StrictMode
import android.support.constraint.ConstraintLayout
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.util.Log
import android.view.*
import android.widget.*
import com.cnr_isac.oldmusa.Login.Companion.api
import com.cnr_isac.oldmusa.R.layout.*
import com.squareup.timessquare.CalendarPickerView
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.activity_museum.*
import kotlinx.android.synthetic.main.add_museum.*
import kotlinx.android.synthetic.main.add_museum.view.*
import kotlinx.android.synthetic.main.add_sensor.*
import kotlinx.android.synthetic.main.add_sensor.view.*
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Date
import java.util.Calendar
import android.widget.Toast



class Museum : AppCompatActivity() {

    lateinit var calendar: CalendarPickerView
        private set

    lateinit var mDrawerLayout: DrawerLayout
        private set

    lateinit var mToggle: ActionBarDrawerToggle
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(activity_museum)

        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        // get site
        val site = api.getSite(intent.getLongExtra("site", -1))
        val list = ArrayList<String>()
        for (sensor in site.sensors) {
            list.add(sensor.name ?: "null")
        }

        Log.e("test", list.toString())

        val listView = findViewById<ListView>(R.id.SensorList)

        val adapter = ArrayAdapter<String>(this, list_sensor_item, list)
        listView.adapter = adapter

        // add event listener to array items
        listView.onItemClickListener = AdapterView.OnItemClickListener { _, view, _, _ ->
            val mBuilder = AlertDialog.Builder(this)
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

            /*calendar = d.findViewById<CalendarPickerView>(R.id.calendar_view)
            val today = Date()
            calendar.init(today, nextYear.time)
                .withSelectedDate(today)
                .inMode(CalendarPickerView.SelectionMode.RANGE)
            calendar.highlightDates(getHolidays())

            // submit graph button listener
            /*d.create.setOnClickListener { view -> // create = bottone invio
                d.dismiss()
            }*/*/
        }

        // open map options modal
        addMapbutton.setOnClickListener {
            val mDialogView = LayoutInflater.from(this).inflate(add_map, null)

            val mBuilder = AlertDialog.Builder(this)
                .setView(mDialogView)
                .setTitle("Aggiungi un sensore")

            val mAlertDialog = mBuilder.show()

            mDialogView.AddButtonM.setOnClickListener {
                mAlertDialog.dismiss()
            }
        }

        // setup drawer layout for modals
        mDrawerLayout = findViewById(R.id.drawerSite)
        mToggle = ActionBarDrawerToggle(this, mDrawerLayout, R.string.open, R.string.close)
        mDrawerLayout.addDrawerListener(mToggle)
        mToggle.syncState()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // open sensor options modal
        addSensorbutton.setOnClickListener{
            //val mDialogView = LayoutInflater.from(this).inflate(R.layout.add_museum, null)

            val mBuilder = AlertDialog.Builder(this)
            mBuilder.setTitle("Aggiungi sensore")
            val d = mBuilder.setView(LayoutInflater.from(this).inflate(add_sensor, null)).create()
            val lp = WindowManager.LayoutParams()
            lp.copyFrom(d.window!!.attributes)
            lp.title = "Aggiungi sensore"
            lp.width = (resources.displayMetrics.widthPixels * 0.90).toInt()
            lp.height = (resources.displayMetrics.heightPixels * 0.80).toInt()
            d.show()
            d.window!!.attributes = lp

            d.AddButtonS.setOnClickListener { view ->
                d.dismiss()
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
        when (id) {
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
        }
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

}
