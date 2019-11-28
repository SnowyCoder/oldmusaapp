package it.cnr.oldmusa.fragments

import android.os.Bundle
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
import it.cnr.oldmusa.R
import it.cnr.oldmusa.UpdateUserAccessMutation
import it.cnr.oldmusa.UserFullAccesQuery
import it.cnr.oldmusa.util.GraphQlUtil.mutate
import it.cnr.oldmusa.util.GraphQlUtil.query
import it.cnr.oldmusa.util.AndroidUtil.useLoadingBar
import it.cnr.oldmusa.util.StreamUtil.forEachTrue


class UserAccessEditFragment : Fragment(), AdapterView.OnItemClickListener {

    val args: UserAccessEditFragmentArgs by navArgs()

    lateinit var listView: ListView
    lateinit var saveButton: Button

    var siteChanges = SparseBooleanArray()
    var changeCount = 0

    var fullSites: List<UserFullAccesQuery.Site> = emptyList()
    lateinit var user: UserFullAccesQuery.User
    var initialAccess: Set<Int> = emptySet()

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

        query(UserFullAccesQuery(args.userId)).onResult { data ->
            fullSites = data.sites()
            initialAccess = data.user().sites().map { it.id() }.toHashSet()
            val indices = IntArray(data.user().sites().size)

            var i = 0

            fullSites.forEachIndexed { index, site ->
                if (site.id() in initialAccess) {
                    indices[i++] = index
                }
            }

            val names = fullSites.map { it.name() ?: "null" }
            listView.adapter = ArrayAdapter(context!!, android.R.layout.simple_list_item_multiple_choice, names)
            listView.choiceMode = ListView.CHOICE_MODE_MULTIPLE
            listView.checkedItemPositions.clear()
            for (index in indices) {
                listView.setItemChecked(index, true)
            }
            siteChanges.clear()
            changeCount = 0
            reloadSaveButtonVisibility()
        }.useLoadingBar(this)
    }

    fun canSave(): Boolean = changeCount > 0

    fun onSave() {
        if (!canSave()) return

        val toGive: MutableList<Int> = ArrayList()
        val toRevoke: MutableList<Int> = ArrayList()

        siteChanges.forEachTrue {
            val site = fullSites[it]

            if (site.id() in initialAccess) {
                toRevoke.add(site.id())
            } else {
                toGive.add(site.id())
            }
        }

        mutate(UpdateUserAccessMutation(args.userId, toGive, toRevoke)).onResult {
            resetAccesses()
        }.useLoadingBar(this)
    }
}
