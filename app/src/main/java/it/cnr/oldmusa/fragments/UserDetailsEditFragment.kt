package it.cnr.oldmusa.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import it.cnr.oldmusa.*
import it.cnr.oldmusa.Account.isAdmin
import it.cnr.oldmusa.type.UserInput
import it.cnr.oldmusa.type.UserUpdateInput
import it.cnr.oldmusa.util.AndroidUtil.toNullableString
import it.cnr.oldmusa.util.AndroidUtil.useLoadingBar
import it.cnr.oldmusa.util.GraphQlUtil.asChar
import it.cnr.oldmusa.util.GraphQlUtil.mutate
import it.cnr.oldmusa.util.GraphQlUtil.query
import it.cnr.oldmusa.util.GraphQlUtil.toPermissionType
import kotlinx.android.synthetic.main.fragment_user_details_edit.*


class UserDetailsEditFragment : Fragment() {

    val args: UserDetailsEditFragmentArgs by navArgs()

    lateinit var user: UserDetailsQuery.User
    var isCurrent: Boolean = false

    var userId: Int = -1

    enum class PermissionType(val char: Char, val friendlyName: String) {
        USER('U', "User"), ADMIN('A', "Admin");

        override fun toString() = friendlyName

        companion object {
            fun fromChar(char: Char): PermissionType = when (char) {
                'U' -> USER
                'A' -> ADMIN
                else -> throw IllegalArgumentException()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_user_details_edit, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        reloadSaveOnChange(username)
        reloadSaveOnChange(newPassword)
        reloadSaveOnChange(newPasswordConfirm)

        permission.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                reloadSaveButtonVisibility()
            }
        }

        if (userId < 0) {
            userId = savedInstanceState?.getInt("userId", args.userId) ?: args.userId
            updateVisibility()
        }



        saveButton.setOnClickListener {
            onSave()
        }

        cancelButton.setOnClickListener {
            this.activity!!.onBackPressed()
        }

        editAccessButton.setOnClickListener {
            findNavController().navigate(
                UserDetailsEditFragmentDirections.actionUserDetailsEditToUserAccessEdit(
                    userId
                )
            )
        }

        deleteButton.setOnClickListener {
            val builder = AlertDialog.Builder(requireActivity())

            builder.setIcon(android.R.drawable.ic_dialog_alert)
            builder.setTitle(getString(R.string.delete_user_dialog_title))
            builder.setMessage(getString(R.string.delete_user_dialog_message))
            builder.setPositiveButton(R.string.delete) { _, _ ->
                mutate(DeleteUserMutation(userId)).onResult {
                    this.activity!!.onBackPressed()
                }
            }
            builder.setNegativeButton(R.string.cancel) {_, _ -> }

            val dialog = builder.create()
            dialog.show()
        }

        permission.adapter = ArrayAdapter<PermissionType>(context!!, android.R.layout.simple_spinner_dropdown_item, PermissionType.values())

        resetDetails()

        if (!isAdmin) {
            username.isEnabled = false
            permission.selectedView?.isEnabled = false
            permission.isEnabled = false
            deleteButton.visibility = View.GONE
            editAccessButton.visibility = View.GONE
        }
    }

    fun updateVisibility() {
        val visibility = if (userId >= 0) View.VISIBLE else View.GONE

        deleteButton.visibility = visibility
        editAccessButton.visibility = visibility
        changePasswordLabel.visibility = visibility
    }

    fun reloadSaveOnChange(text: TextView) {
        text.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                reloadSaveButtonVisibility()
            }
        })
    }

    fun reloadSaveButtonVisibility() {
        saveButton.isEnabled = canSave()
    }


    fun resetDetails() {
        if (userId < 0) return

        query(UserDetailsQuery(userId)).onResult { data ->
            user = data.user()
            isCurrent = data.userMe()!!.id() == user.id()

            username.setText(user.username())
            newPassword.setText("")
            newPasswordConfirm.setText("")
            permission.setSelection(
                PermissionType.fromChar(
                    user.permission().asChar()
                ).ordinal
            )
        }.useLoadingBar(this)
    }

    fun canSave(): Boolean {
        if (userId < 0) {
            // Creating user
            if (username.text.isNullOrBlank()) return false
            if (newPassword.text.isNullOrEmpty()) {
                newPasswordConfirm.error = null
                return false
            }
            if (newPassword.text.toString() != newPasswordConfirm.text.toString()) {
                newPasswordConfirm.error = "Password different"
                return false
            }
            newPasswordConfirm.error = null
            return true
        }
        // Editing user
        if (!::user.isInitialized) return false

        var canSave = false

        if (!newPassword.text.isNullOrEmpty()) {
            if (newPassword.text.toString() != newPasswordConfirm.text.toString()) {
                newPasswordConfirm.error = "Password different"
                return false
            }
            newPasswordConfirm.error = null
            canSave = true
        }

        if (user.username() != username.text.toString() || (user.permission().asChar() != (permission.selectedItem as PermissionType).char)) {
            canSave = isAdmin
        }

        return canSave
    }

    fun onSave() {
        if (!canSave()) return

        val newUsername = username.text.toString()
        val newPermission = (permission.selectedItem as PermissionType).char
        val newPassword = newPassword.text.toString()

        if (userId < 0) {
            mutate(AddUserMutation(
                UserInput.builder()
                    .username(newUsername)
                    .password(newPassword)
                    .permission(newPermission.toPermissionType())
                    .build()
            )).onResult {
                userId = it.addUser().id()
                updateVisibility()
                resetDetails()
            }.useLoadingBar(this)
        } else {
            val updateUsername = if (newUsername == user.username()) null else newUsername
            val updatePermission = if (newPermission == user.permission().asChar()) null else newPermission.toPermissionType()
            val updatePassword = newPassword.toNullableString()

            mutate(
                UpdateUserMutation(
                    userId,
                    UserUpdateInput.builder()
                        .username(updateUsername)
                        .permission(updatePermission)
                        .password(updatePassword)
                        .build()
                )
            ).onResult {
                resetDetails()
            }.useLoadingBar(this)
        }
    }
}
