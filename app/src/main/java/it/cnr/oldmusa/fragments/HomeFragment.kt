package it.cnr.oldmusa.fragments

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import it.cnr.oldmusa.Account.isAdmin
import it.cnr.oldmusa.AddSiteMutation
import it.cnr.oldmusa.R
import it.cnr.oldmusa.SiteListQuery
import it.cnr.oldmusa.util.AndroidUtil.toNullableString
import it.cnr.oldmusa.util.GraphQlUtil.mutate
import it.cnr.oldmusa.util.GraphQlUtil.query
import it.cnr.oldmusa.util.AndroidUtil.useLoadingBar
import kotlinx.android.synthetic.main.add_museum.*
import kotlinx.android.synthetic.main.fragment_home.*


class HomeFragment : Fragment(), SwipeRefreshLayout.OnRefreshListener {


    lateinit var sites: List<SiteListQuery.Site>
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
            val action = HomeFragmentDirections.actionHomeToSite(sites[position].id())
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

        query(SiteListQuery()).onResult { data ->
            this.sites = data.sites()

            val nameList = sites.map { it.name() ?: "null" }

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

        mutate(
            AddSiteMutation.builder()
                .name(museumName.text.toNullableString())
                .idCnr(idCnr.text.toNullableString())
                .build()
        ).onResult {
            reload(this.view!!)
        }.useLoadingBar(this)
    }

    companion object {
        const val TAG = "Home"
    }
}
