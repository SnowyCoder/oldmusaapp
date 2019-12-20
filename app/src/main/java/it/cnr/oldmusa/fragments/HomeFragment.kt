package it.cnr.oldmusa.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import it.cnr.oldmusa.Account.isAdmin
import it.cnr.oldmusa.R
import it.cnr.oldmusa.SiteListQuery
import it.cnr.oldmusa.util.GraphQlUtil.query
import it.cnr.oldmusa.util.AndroidUtil.linkToList
import kotlinx.android.synthetic.main.fragment_home.*


class HomeFragment : Fragment(), SwipeRefreshLayout.OnRefreshListener {

    lateinit var sites: List<SiteListQuery.Site>
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        activity?.title = "OldMusa"

        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // SwipeRefreshLayout
        swipeRefreshLayout = swipeContainer as SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener(this)

        swipeRefreshLayout.post {
            swipeRefreshLayout.isRefreshing = true
            reload(view)
        }
        swipeRefreshLayout.linkToList(siteList)

        // Buttons
        addSiti.setOnClickListener {
            findNavController().navigate(
                HomeFragmentDirections.actionHomeToCreateSiteFragment()
            )
        }

        siteList.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
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
            siteList.adapter = adapter

            emptyText.visibility = if (sites.isEmpty()) View.VISIBLE else View.GONE


            swipeRefreshLayout.isRefreshing = false
        }
    }

    companion object {
        const val TAG = "Home"
    }
}
