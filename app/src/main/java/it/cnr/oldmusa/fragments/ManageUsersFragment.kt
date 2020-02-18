package it.cnr.oldmusa.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import it.cnr.oldmusa.R
import it.cnr.oldmusa.UserListQuery
import it.cnr.oldmusa.util.AndroidUtil.useLoadingBar
import it.cnr.oldmusa.util.GraphQlUtil.query
import kotlinx.android.synthetic.main.fragment_users.*


class ManageUsersFragment : Fragment() {

    lateinit var users: List<UserListQuery.User>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        return inflater.inflate(R.layout.fragment_users, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userList.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val action =
                ManageUsersFragmentDirections.actionManageUsersToUserDetailsEdit(
                    users[position].id()
                )
            view.findNavController().navigate(action)
        }

        addUser.setOnClickListener {
            val action = ManageUsersFragmentDirections.actionManageUsersToUserDetailsEdit(-1)
            view.findNavController().navigate(action)
        }

        loadUsers()
    }

    fun loadUsers() {
        query(UserListQuery()).onResult { data ->
            this.users = data.users()

            val nameList = this.users.map { it.username() }

            val adapter = ArrayAdapter<String>(context!!, R.layout.list_museum_item, nameList)
            userList.adapter = adapter
        }.useLoadingBar(this)
    }

    companion object {
        const val TAG = "ManageUsers"
    }
}
