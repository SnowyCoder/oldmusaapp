package com.cnr_isac.oldmusa

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
import com.cnr_isac.oldmusa.api.ApiUser
import com.cnr_isac.oldmusa.api.User
import com.cnr_isac.oldmusa.util.ApiUtil.api
import com.cnr_isac.oldmusa.util.ApiUtil.query
import com.cnr_isac.oldmusa.util.ApiUtil.useLoadingBar
import kotlinx.android.synthetic.main.add_museum.*


class ManageUsers : Fragment() {

    lateinit var users: List<User>
    lateinit var listView: ListView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        val view = inflater.inflate(R.layout.fragment_users, container, false)


        listView = view.findViewById(R.id.ListUser)

        loadUsers()

        listView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val action = ManageUsersDirections.actionManageUsersToUserDetailsEdit(users[position].id)
            view.findNavController().navigate(action)
        }

        view.findViewById<ImageButton>(R.id.addUser).setOnClickListener {
            //val mDialogView = LayoutInflater.from(this).inflate(R.layout.add_museum, null)

            val mBuilder = AlertDialog.Builder(context!!)
            mBuilder.setTitle("Aggiungi utente")
            val dialog = mBuilder.setView(LayoutInflater.from(context!!).inflate(R.layout.add_user, null)).create()
            val lp = WindowManager.LayoutParams()
            lp.copyFrom(dialog.window!!.attributes)
            lp.title = "Aggiungi utente"
            lp.width = (resources.displayMetrics.widthPixels * 0.80).toInt()
            lp.height = (resources.displayMetrics.heightPixels * 0.50).toInt()
            dialog.show()
            dialog.window!!.attributes = lp

            dialog.AddButtonM.setOnClickListener { view ->
                dialog.dismiss()
                addUser(dialog)
            }
        }

        return view
    }

    fun loadUsers() {
        query {
            api.getUsers()
        }.onResult { users ->
            this.users = users

            val nameList = this.users.map { it.username }

            Log.e(TAG, nameList.toString())

            val adapter = ArrayAdapter<String>(context!!, R.layout.list_museum_item, nameList)
            listView.adapter = adapter
        }.useLoadingBar(this)
    }

    fun addUser(dialog: Dialog) {
        val username = dialog.findViewById<EditText>(R.id.username)!!.text
        val password = dialog.findViewById<EditText>(R.id.password)!!.text

        if (username.isNullOrEmpty() or password.isNullOrEmpty()) return

        query {
            val newUser = api.addUser(ApiUser(
                username = username.toString(),
                password = password.toString()
            ))
        }.onResult {
            // TODO: view user details(?)
            loadUsers()
        }.useLoadingBar(this)
    }

    companion object {
        const val TAG = "Home"
    }
}
