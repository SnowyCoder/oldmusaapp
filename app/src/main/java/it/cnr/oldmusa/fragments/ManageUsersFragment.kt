package it.cnr.oldmusa.fragments

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import it.cnr.oldmusa.AddUserMutation
import it.cnr.oldmusa.R
import it.cnr.oldmusa.UserListQuery
import it.cnr.oldmusa.type.PermissionType
import it.cnr.oldmusa.type.UserInput
import it.cnr.oldmusa.util.AndroidUtil.useLoadingBar
import it.cnr.oldmusa.util.GraphQlUtil.mutate
import it.cnr.oldmusa.util.GraphQlUtil.query
import kotlinx.android.synthetic.main.fragment_create_site.*


class ManageUsersFragment : Fragment() {

    lateinit var users: List<UserListQuery.User>
    lateinit var listView: ListView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        val view = inflater.inflate(R.layout.fragment_users, container, false)


        listView = view.findViewById(R.id.ListUser)

        loadUsers()

        listView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val action =
                ManageUsersFragmentDirections.actionManageUsersToUserDetailsEdit(
                    users[position].id()
                )
            view.findNavController().navigate(action)
        }

        view.findViewById<ImageButton>(R.id.addUser).setOnClickListener {
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

            dialog.add.setOnClickListener { view ->
                dialog.dismiss()
                addUser(dialog)
            }
        }

        return view
    }

    fun loadUsers() {
        query(UserListQuery()).onResult { data ->
            this.users = data.users()

            val nameList = this.users.map { it.username() }

            val adapter = ArrayAdapter<String>(context!!,
                R.layout.list_museum_item, nameList)
            listView.adapter = adapter
        }.useLoadingBar(this)
    }

    fun addUser(dialog: Dialog) {
        val username = dialog.findViewById<EditText>(R.id.username)!!.text
        val password = dialog.findViewById<EditText>(R.id.password)!!.text

        if (username.isNullOrEmpty() or password.isNullOrEmpty()) return

        mutate(AddUserMutation(
            UserInput.builder()
                .username(username.toString())
                .password(password.toString())
                .permission(PermissionType.USER)
                .build()
        )).onResult {
            val action =
                ManageUsersFragmentDirections.actionManageUsersToUserDetailsEdit(it.addUser().id())
            view!!.findNavController().navigate(action)
        }.useLoadingBar(this)
    }

    companion object {
        const val TAG = "ManageUsers"
    }
}
