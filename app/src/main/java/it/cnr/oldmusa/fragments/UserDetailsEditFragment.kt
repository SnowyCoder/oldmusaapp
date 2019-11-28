package it.cnr.oldmusa.fragments

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
import it.cnr.oldmusa.Account.isAdmin
import it.cnr.oldmusa.DeleteUserMutation
import it.cnr.oldmusa.R
import it.cnr.oldmusa.UpdateUserMutation
import it.cnr.oldmusa.UserDetailsQuery
import it.cnr.oldmusa.type.UserUpdateInput
import it.cnr.oldmusa.util.AndroidUtil.toNullableString
import it.cnr.oldmusa.util.GraphQlUtil.asChar
import it.cnr.oldmusa.util.GraphQlUtil.mutate
import it.cnr.oldmusa.util.GraphQlUtil.query
import it.cnr.oldmusa.util.GraphQlUtil.toPermissionType
import it.cnr.oldmusa.util.AndroidUtil.useLoadingBar


class UserDetailsEditFragment : Fragment() {

    val args: UserDetailsEditFragmentArgs by navArgs()

    lateinit var usernameEditText: EditText
    lateinit var permissionSpinner: Spinner
    lateinit var passwordEditText: EditText
    lateinit var passwordConfirmEditText: EditText

    lateinit var saveButton: Button
    lateinit var deleteButton: Button

    lateinit var user: UserDetailsQuery.User
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
            findNavController().navigate(
                UserDetailsEditFragmentDirections.actionUserDetailsEditToUserAccessEdit(
                    args.userId
                )
            )
        }

        deleteButton.setOnClickListener {
            // TODO: are you sure?
            mutate(DeleteUserMutation(args.userId)).onResult {
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

        query(UserDetailsQuery(args.userId)).onResult { data ->
            user = data.user()
            isCurrent = data.userMe().id() == user.id()

            usernameEditText.setText(user.username())
            passwordEditText.setText("")
            passwordConfirmEditText.setText("")
            permissionSpinner.setSelection(
                PermissionType.fromChar(
                    user.permission().asChar()
                ).ordinal)
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

        if (user.username() != usernameEditText.text.toString() || (user.permission().asChar() != (permissionSpinner.selectedItem as PermissionType).char)) {
            canSave = isAdmin
        }

        return canSave
    }

    fun onSave() {
        if (!canSave()) return

        val newUsername = usernameEditText.text.toString()
        val newPermission = (permissionSpinner.selectedItem as PermissionType).char
        val newPassword = passwordEditText.text.toString()

        val updateUsername = if (newUsername == user.username()) null else newUsername
        val updatePermission = if (newPermission == user.permission().asChar()) null else newPermission.toPermissionType()
        val updatePassword = newPassword.toNullableString()

        mutate(UpdateUserMutation(
            args.userId,
            UserUpdateInput.builder()
                .username(updateUsername)
                .permission(updatePermission)
                .password(updatePassword)
                .build()
        )).onResult {
            resetDetails()
        }.useLoadingBar(this)
    }
}
