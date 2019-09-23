package com.cnr_isac.oldmusa.fragments

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.cnr_isac.oldmusa.Account.isAdmin
import com.cnr_isac.oldmusa.R
import com.cnr_isac.oldmusa.api.Site
import com.cnr_isac.oldmusa.util.ApiUtil.api
import com.cnr_isac.oldmusa.util.ApiUtil.query
import com.cnr_isac.oldmusa.util.ApiUtil.useLoadingBar
import kotlinx.android.synthetic.main.add_museum.*
import kotlinx.android.synthetic.main.fragment_home.*


class HomeFragment : Fragment(), SwipeRefreshLayout.OnRefreshListener {


    lateinit var sites: List<Site>
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        activity?.title = "OldMusa"
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        view.findViewById<ImageButton>(R.id.addSiti).setOnClickListener{
            //val mDialogView = LayoutInflater.from(this).inflate(R.layout.add_museum, null)

            val mBuilder = AlertDialog.Builder(context!!)
            mBuilder.setTitle("Aggiungi museo")
            val dialogView = LayoutInflater.from(context!!).inflate(R.layout.add_museum, null)
            val dialog = mBuilder.setView(dialogView).create()
            val lp = WindowManager.LayoutParams()
            lp.copyFrom(dialog.window!!.attributes)
            lp.title = "Aggiungi museo"
            lp.width = (resources.displayMetrics.widthPixels * 0.80).toInt()
            lp.height = (resources.displayMetrics.heightPixels * 0.50).toInt()
            dialog.show()
            dialog.window!!.attributes = lp

            dialog.AddButtonM.setOnClickListener { view ->
                dialog.dismiss()
                addMuseum(dialog)
            }
        }

        // SwipeRefreshLayout
        swipeRefreshLayout = view.findViewById(R.id.swipeContainer) as SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener(this)

        swipeRefreshLayout.post {
            swipeRefreshLayout.isRefreshing = true
            reload(view)
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        museumList.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val action = HomeFragmentDirections.actionHomeToSite(sites[position].id)
            view.findNavController().navigate(action)
        }
    }

    override fun onRefresh() {
        reload(view!!)
    }


    fun reload(view: View) {
        // permission
        if (isAdmin) {
            val buttonVisible = view.findViewById<ImageButton>(R.id.addSiti)
            buttonVisible.visibility = View.VISIBLE
        }

        query {
            api.getSites()
        }.onResult { sites ->
            this.sites = sites

            val nameList = sites.map { it.name ?: "null" }

            Log.e(TAG, nameList.toString())

            val adapter = ArrayAdapter<String>(context!!,
                R.layout.list_museum_item, nameList)
            museumList.adapter = adapter

            emptyText.visibility = if (sites.isEmpty()) View.VISIBLE else View.GONE


            swipeRefreshLayout.isRefreshing = false
        }
    }

    fun addMuseum (view: Dialog) {
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
        }.onResult {
            reload(this.view!!)
        }.useLoadingBar(this)
    }

    companion object {
        const val TAG = "Home"
    }
}
