package com.cnr_isac.oldmusa

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_about.*
import android.content.Intent
import android.net.Uri
import android.widget.Button
import android.widget.TextView


class About : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_about, container, false)
        view.findViewById<TextView>(R.id.linkGitHub).setOnClickListener{
            goToUrl()
        }
        return view
    }

    private fun goToUrl() {
        val uriUrl = Uri.parse("https://github.com/OldMusa-5H/")
        val launchBrowser = Intent(Intent.ACTION_VIEW, uriUrl)
        startActivity(launchBrowser)
    }

}

