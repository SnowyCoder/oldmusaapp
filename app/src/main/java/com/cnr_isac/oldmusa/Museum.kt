package com.cnr_isac.oldmusa

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import kotlinx.android.synthetic.main.activity_museum.*

class Museum : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_museum)

        addMapbutton.setOnClickListener {
            val mDialogView = LayoutInflater.from(this).inflate(R.layout.add_map, null)
        }
    }
}
