package com.cnr_isac.oldmusa.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import android.content.Intent
import android.net.Uri
import android.widget.TextView
import com.cnr_isac.oldmusa.Constants
import com.cnr_isac.oldmusa.Constants.goToGithubPage
import com.cnr_isac.oldmusa.R


class AboutFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_about, container, false)
        view.findViewById<TextView>(R.id.linkGitHub).setOnClickListener{
            goToGithubPage(context!!)
        }
        return view
    }



}

