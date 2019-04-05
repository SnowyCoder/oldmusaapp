package com.cnr_isac.oldmusa

import android.app.AlertDialog
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.*
import com.cnr_isac.oldmusa.Login.Companion.api
import kotlinx.android.synthetic.main.activity_museum.*
import kotlinx.android.synthetic.main.add_museum.view.*
import kotlinx.android.synthetic.main.add_sensor.view.*
import java.util.ArrayList

class Museum : AppCompatActivity() {

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

        addSensorbutton.setOnClickListener {
            val mDialogView = LayoutInflater.from(this).inflate(R.layout.add_sensor, null)

            val mBuilder = AlertDialog.Builder(this)
                .setView(mDialogView)
                .setTitle("Aggiungi un sensore")

            val mAlertDialog = mBuilder.show()

            mDialogView.AddButtonS.setOnClickListener {
                mAlertDialog.dismiss()
                //val name = mDialogView.nameMuseum.text.toString()
                //val channels: List<Int>

            }

            mDialogView.CloseS.setOnClickListener {
                mAlertDialog.dismiss()
            }

        }
    }


}
