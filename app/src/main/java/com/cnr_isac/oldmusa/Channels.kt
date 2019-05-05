package com.cnr_isac.oldmusa

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ListView
import androidx.fragment.app.Fragment
import com.cnr_isac.oldmusa.util.ApiUtil.isAdmin

class Channels : Fragment(){

    private lateinit var listView: ListView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        val view = inflater.inflate(R.layout.fragment_sensor, container, false)

        /*isAdmin {
            if (!it) return@isAdmin

            val buttonVisible1 = view.findViewById<ImageButton>(R.id.addChannbutton)
            buttonVisible1.visibility=View.VISIBLE
        }*/

        //listView = view.findViewById(R.id.channelList)



        return view
    }
}