package com.cnr_isac.oldmusa

import android.os.Bundle
import android.util.Log
import android.util.SparseBooleanArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.cnr_isac.oldmusa.api.Site
import com.cnr_isac.oldmusa.api.User
import com.cnr_isac.oldmusa.util.ApiUtil.api
import com.cnr_isac.oldmusa.util.ApiUtil.query
import com.cnr_isac.oldmusa.util.ApiUtil.withLoading
import com.cnr_isac.oldmusa.util.StreamUtil.forEachTrue


class UserAccessEdit : Fragment(), AdapterView.OnItemClickListener {

    val args: UserAccessEditArgs by navArgs()

    lateinit var listView: ListView
    lateinit var saveButton: Button

    var siteChanges = SparseBooleanArray()
    var changeCount = 0

    var fullSites: List<Site> = emptyList()
    lateinit var user: User
    var initialAccess: Set<Long> = emptySet()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_user_access_edit, container, false)

        listView = view.findViewById(R.id.accessList)

        saveButton = view.findViewById(R.id.saveButton)
        val cancelButton = view.findViewById<Button>(R.id.cancelButton)

        listView.onItemClickListener = this


        saveButton.setOnClickListener {
            onSave()
        }

        cancelButton.setOnClickListener {
            this.activity!!.onBackPressed()
        }

        resetAccesses()

        return view
    }

    override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        val changed = siteChanges.get(position)
        siteChanges.put(position, !changed)
        changeCount += if (!changed) 1 else -1

        reloadSaveButtonVisibility()
    }

    fun reloadSaveButtonVisibility() {
        saveButton.isEnabled = canSave()
    }


    fun resetAccesses() {
        val userId = args.userId

        query {
            fullSites = api.getSites()
            user = api.getUser(userId)
            val access = user.access
            initialAccess = access.map { it.id }.toHashSet()

            val indices = IntArray(access.size)

            var i = 0

            fullSites.forEachIndexed { index, site ->
                if (site.id in initialAccess) {
                    indices[i++] = index
                }
            }
            indices
        }.onResult { indices ->
            val names = fullSites.map { it.name ?: "null" }
            listView.adapter = ArrayAdapter(context!!, android.R.layout.simple_list_item_multiple_choice, names)
            listView.choiceMode = ListView.CHOICE_MODE_MULTIPLE
            listView.checkedItemPositions.clear()
            for (i in indices) {
                listView.setItemChecked(i, true)
            }
            siteChanges.clear()
            changeCount = 0
            reloadSaveButtonVisibility()
        }.withLoading(this)
    }

    fun canSave(): Boolean = changeCount > 0

    fun onSave() {
        if (!canSave()) return

        query {
            siteChanges.forEachTrue {
                val site = fullSites[it]

                if (site.id in initialAccess) {
                    user.removeAccess(site)
                } else {
                    user.addAccess(site)
                }
            }
        }.onDone {
            resetAccesses()
        }.withLoading(this)
    }
}
