package com.cnr_isac.oldmusa

import android.app.AlertDialog
import android.content.Intent
import android.drm.DrmStore
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.util.Log
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.*
import com.cnr_isac.oldmusa.Login.Companion.api
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.activity_museum.*
import kotlinx.android.synthetic.main.add_museum.*
import kotlinx.android.synthetic.main.add_museum.view.*
import kotlinx.android.synthetic.main.add_sensor.*
import kotlinx.android.synthetic.main.add_sensor.view.*
import java.util.ArrayList

class Museum : AppCompatActivity() {

    lateinit var mDrawerLayout: DrawerLayout
        private set

    lateinit var mToggle: ActionBarDrawerToggle
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_museum)

        val site = api.getSite(intent.getLongExtra("site", -1))
        val list = ArrayList<String>()
        for (sensor in site.sensors) {
            list.add(sensor.name ?: "null")
        }

        Log.e("test", list.toString())

        val listView = findViewById<ListView>(R.id.SensorList)

        val adapter = ArrayAdapter<String>(this, R.layout.list_sensor_item, list)
        listView.adapter = adapter

        listView.onItemClickListener = AdapterView.OnItemClickListener { _, _, _, _ ->
        }


        addMapbutton.setOnClickListener {
            val mDialogView = LayoutInflater.from(this).inflate(R.layout.add_map, null)

            val mBuilder = AlertDialog.Builder(this)
                .setView(mDialogView)
                .setTitle("Aggiungi un sensore")

            val mAlertDialog = mBuilder.show()

            mDialogView.AddButtonM.setOnClickListener {
                mAlertDialog.dismiss()
            }
        }

        mDrawerLayout = findViewById(R.id.drawerSite)
        mToggle = ActionBarDrawerToggle(this, mDrawerLayout, R.string.open, R.string.close)
        mDrawerLayout.addDrawerListener(mToggle)
        mToggle.syncState()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        addSensorbutton.setOnClickListener{
            //val mDialogView = LayoutInflater.from(this).inflate(R.layout.add_museum, null)

            val mBuilder = AlertDialog.Builder(this)
            mBuilder.setTitle("Aggiungi sensore")
            val d = mBuilder.setView(LayoutInflater.from(this).inflate(R.layout.add_sensor, null)).create()
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


}
