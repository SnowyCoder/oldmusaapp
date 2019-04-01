package com.cnr_isac.oldmusa

import android.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.CheckBox
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_museum.*
import kotlinx.android.synthetic.main.add_museum.view.*
import kotlinx.android.synthetic.main.add_sensor.view.*

class Museum : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_museum)

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
