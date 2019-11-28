package it.cnr.oldmusa.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import it.cnr.oldmusa.CurrentUserQuery
import it.cnr.oldmusa.LoginMutation
import it.cnr.oldmusa.R
import it.cnr.oldmusa.api.graphql.WrongPasswordGraphQlException
import it.cnr.oldmusa.firebase.FirebaseUtil
import it.cnr.oldmusa.util.GraphQlUtil.apollo
import it.cnr.oldmusa.util.GraphQlUtil.apolloCall
import it.cnr.oldmusa.util.GraphQlUtil.asChar
import it.cnr.oldmusa.util.GraphQlUtil.handleError
import it.cnr.oldmusa.util.AndroidUtil.useLoadingBar
import kotlinx.android.synthetic.main.fragment_login.*
import java.net.ConnectException


class LoginFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        activity?.title = "Login"


        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loginButton.setOnClickListener {
            login()
        }

        // If the api is already logged in, skip
        apolloCall(apollo.query(CurrentUserQuery()), manageError = false)
            .onResult {
                val data = it.userMe()
                goToHome(data.id(), data.username(), data.permission().asChar())
            }
            .useLoadingBar(this)
    }

    fun login() {
        val name = user.text
        val pass = pass.text

        if (name.isNullOrEmpty() or pass.isNullOrEmpty()) return

        val context = this.context!!

        this.apolloCall(this.apollo.mutate(LoginMutation(name.toString(), pass.toString())), manageError = false)
            .onError {
                when (it) {
                    is WrongPasswordGraphQlException -> {
                        val dialogBuilder = AlertDialog.Builder(context)
                        dialogBuilder.setMessage("Username o Password errati")
                            .setPositiveButton("OK") { dialogInterface, _ ->
                                dialogInterface.cancel()
                            }
                        val alert = dialogBuilder.create()
                        alert.setTitle("Error")
                        alert.show()
                    }
                    is ConnectException -> {
                        val dialogBuilder = AlertDialog.Builder(context)
                        dialogBuilder.setMessage("Errore nella connesione al server")
                            .setPositiveButton("OK") { dialogInterface, _ ->
                                dialogInterface.cancel()
                            }
                        val alert = dialogBuilder.create()
                        alert.setTitle("Error")
                        alert.show()
                        Log.e(TAG, "Error connecting to the server", it)
                    }
                    else -> handleError(context, it)
                }
            }
            .onResult {
                FirebaseUtil.publishFCMToken(apollo) // Publish the current FCM token to the server (used to send notifications)

                val data = it.login()
                goToHome(data.id(), name.toString(), data.permission().asChar())
            }.useLoadingBar(this)
    }

    fun goToHome(id: Int, username: String, permission: Char) {
        (activity as LoginCompleteListener).onLoginComplete(id, username, permission)
    }

    interface LoginCompleteListener {
        fun onLoginComplete(id: Int, username: String, permission: Char)
    }


    companion object {
        val TAG = "Login"
    }
}