package it.cnr.oldmusa.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import android.widget.TextView
import it.cnr.oldmusa.Constants.goToGithubPage
import it.cnr.oldmusa.R


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

