package com.cnr_isac.oldmusa

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.cnr_isac.oldmusa.api.Site
import com.cnr_isac.oldmusa.util.ApiUtil.api
import com.cnr_isac.oldmusa.util.ApiUtil.isAdmin
import com.cnr_isac.oldmusa.util.ApiUtil.query
import com.cnr_isac.oldmusa.util.ApiUtil.withLoading
import kotlinx.android.synthetic.main.add_museum.*
import kotlinx.android.synthetic.main.fragment_home.*


class Home : Fragment() {

    lateinit var sites: List<Site>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        val view = inflater.inflate(R.layout.fragment_home, container, false)


        val listView = view.findViewById<ListView>(R.id.ListMuseum)

        // permission
        isAdmin {
            if (!it) return@isAdmin

            val buttonVisible = view.findViewById<ImageButton>(R.id.addSiti)
            buttonVisible.visibility = View.VISIBLE
        }

        query {
            api.getSites()
        }.onResult { sites ->
            this.sites = sites

            val nameList = sites.map { it.name ?: "null" }

            Log.e(TAG, nameList.toString())

            val adapter = ArrayAdapter<String>(context!!, R.layout.list_museum_item, nameList)
            listView.adapter = adapter
        }.withLoading(this)

        listView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val action = HomeDirections.actionHomeToSite(sites[position].id)
            view.findNavController().navigate(action)
        }

        view.findViewById<ImageButton>(R.id.addSiti).setOnClickListener{
            //val mDialogView = LayoutInflater.from(this).inflate(R.layout.add_museum, null)

            val mBuilder = AlertDialog.Builder(context!!)
            mBuilder.setTitle("Aggiungi museo")
            val d = mBuilder.setView(LayoutInflater.from(context!!).inflate(R.layout.add_museum, null)).create()
            val lp = WindowManager.LayoutParams()
            lp.copyFrom(d.window!!.attributes)
            lp.title = "Aggiungi museo"
            lp.width = (resources.displayMetrics.widthPixels * 0.80).toInt()
            lp.height = (resources.displayMetrics.heightPixels * 0.50).toInt()
            d.show()
            d.window!!.attributes = lp

            d.AddButtonM.setOnClickListener { view ->
                d.dismiss()
                addMuseum(view)
            }
        }

        return view
    }

    fun addMuseum (view: View) {
        Log.e("start", "start add museum")
        val museumName = view.findViewById<EditText>(R.id.nameMuseum)!!
        val idCnr = view.findViewById<EditText>(R.id.idCnr)!!
        query {
            val newMuseum = api.addSite()
            newMuseum.name = museumName.text.toString()
            Log.e("text", newMuseum.name)
            newMuseum.idCnr = idCnr.text.toString()
            Log.e("text", newMuseum.idCnr)
            newMuseum.commit()
        }.withLoading(this)
    }

    companion object {
        const val TAG = "Home"
    }
}
