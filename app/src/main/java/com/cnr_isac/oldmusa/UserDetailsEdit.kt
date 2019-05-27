package com.cnr_isac.oldmusa

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.cnr_isac.oldmusa.Account.isAdmin
import com.cnr_isac.oldmusa.api.User
import com.cnr_isac.oldmusa.util.ApiUtil.api
import com.cnr_isac.oldmusa.util.ApiUtil.query
import com.cnr_isac.oldmusa.util.ApiUtil.useLoadingBar


class UserDetailsEdit : Fragment() {

    val args: UserDetailsEditArgs by navArgs()

    lateinit var usernameEditText: EditText
    lateinit var permissionSpinner: Spinner
    lateinit var passwordEditText: EditText
    lateinit var passwordConfirmEditText: EditText

    lateinit var saveButton: Button
    lateinit var deleteButton: Button

    lateinit var user: User
    var isCurrent: Boolean = false

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
        val view = inflater.inflate(R.layout.fragment_user_details_edit, container, false)

        usernameEditText = view.findViewById(R.id.username)
        permissionSpinner = view.findViewById(R.id.permission)
        passwordEditText = view.findViewById(R.id.newPassword)
        passwordConfirmEditText = view.findViewById(R.id.newPasswordConfirm)

        saveButton = view.findViewById(R.id.saveButton)
        val cancelButton = view.findViewById<Button>(R.id.cancelButton)
        val editAccessButton = view.findViewById<Button>(R.id.editAccessButton)

        deleteButton = view.findViewById(R.id.deleteButton)

        reloadSaveOnChange(usernameEditText)
        reloadSaveOnChange(passwordEditText)
        reloadSaveOnChange(passwordConfirmEditText)
        permissionSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                reloadSaveButtonVisibility()
            }
        }

        saveButton.setOnClickListener {
            onSave()
        }

        cancelButton.setOnClickListener {
            this.activity!!.onBackPressed()
        }

        editAccessButton.setOnClickListener {
            findNavController().navigate(UserDetailsEditDirections.actionUserDetailsEditToUserAccessEdit(args.userId))
        }

        deleteButton.setOnClickListener {
            query {
                user.delete()
            }.onResult {
                this.activity!!.onBackPressed()
            }
        }

        val permission = view.findViewById<Spinner>(R.id.permission)
        permission.adapter = ArrayAdapter<PermissionType>(context!!, android.R.layout.simple_spinner_dropdown_item, PermissionType.values())

        resetDetails()

        if (!isAdmin) {
            usernameEditText.isEnabled = false
            permissionSpinner.selectedView?.isEnabled = false
            permissionSpinner.isEnabled = false
            deleteButton.visibility = View.GONE
            editAccessButton.visibility = View.GONE
        }

        return view
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
        val userId = args.userId

        query {
            user = api.getUser(userId)
            isCurrent = api.getMe().id == user.id
        }.onResult {
            usernameEditText.setText(user.username)
            passwordEditText.setText("")
            passwordConfirmEditText.setText("")
            permissionSpinner.setSelection(PermissionType.fromChar(user.permission).ordinal)
        }.useLoadingBar(this)
    }

    fun canSave(): Boolean {
        if (!::user.isInitialized) return false

        var canSave = false

        if (!passwordEditText.text.isNullOrEmpty()) {
            if (passwordEditText.text.toString() != passwordConfirmEditText.text.toString()) {
                passwordConfirmEditText.error = "Password different"
                return false
            }
            passwordConfirmEditText.error = null
            canSave = true
        }

        if (user.username != usernameEditText.text.toString() || (user.permission != (permissionSpinner.selectedItem as PermissionType).char)) {
            canSave = isAdmin
        }

        return canSave
    }

    fun onSave() {
        if (!canSave()) return

        val newUsername = usernameEditText.text.toString()
        val newPermission = (permissionSpinner.selectedItem as PermissionType).char
        val newPassword = passwordEditText.text.toString()

        query {
            if (user.username != newUsername || user.permission != newPermission) {
                user.username = newUsername
                user.permission = newPermission
            }

            if (newPassword.isNotEmpty()) {
                user.changePassword(newPassword)
            }

            user.commit()
            if (isCurrent && newPassword.isNotEmpty()) {
                api.logout()
                api.login(user.username, newPassword)
                Account.saveToken(context!!)
            }
        }.onDone {
            resetDetails()
        }.useLoadingBar(this)
    }
}
