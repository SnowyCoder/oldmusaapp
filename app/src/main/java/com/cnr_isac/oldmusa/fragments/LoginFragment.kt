package com.cnr_isac.oldmusa.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.cnr_isac.oldmusa.Account
import com.cnr_isac.oldmusa.R
import com.cnr_isac.oldmusa.api.RestException
import com.cnr_isac.oldmusa.api.User
import com.cnr_isac.oldmusa.util.ApiUtil.api
import com.cnr_isac.oldmusa.firebase.FirebaseUtil
import com.cnr_isac.oldmusa.util.ApiUtil
import com.cnr_isac.oldmusa.util.ApiUtil.handleError
import com.cnr_isac.oldmusa.util.ApiUtil.query
import com.cnr_isac.oldmusa.util.ApiUtil.useLoadingBar
import kotlinx.android.synthetic.main.fragment_login.*
import java.lang.Exception
import java.net.ConnectException


class LoginFragment : Fragment() {

    /**
     * Checks if the login works by querying the current user
     * if the query fails it does not crash (instead it logs out)
     */
    private fun checkGetMe(): User? {
        // Try to get the current user
        if (api.getCurrentToken().isNullOrBlank()) return null
        return try {
            api.getMe()
        } catch (e: Exception) {
            api.logout()
            Account.saveToken(context!!)
            null
        }
    }

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
        query { checkGetMe() }.onResult {
            if (it != null) goToHome(it.username, it.permission)
        }.useLoadingBar(this)
    }

    fun login() {
        val name = user.text
        val pass = pass.text

        if (name.isNullOrEmpty() or pass.isNullOrEmpty()) return

        val context = this.context!!

        ApiUtil.RawQuery {
            api.login(name.toString(), pass.toString())
            api.getMe()
        }.onError {
            when {
                it is RestException && it.code == 401 -> {
                    val dialogBuilder = AlertDialog.Builder(context)
                    dialogBuilder.setMessage("Username o Password errati")
                        .setPositiveButton("OK") { dialogInterface, _ ->
                            dialogInterface.cancel()
                        }
                    val alert = dialogBuilder.create()
                    alert.setTitle("Error")
                    alert.show()
                }
                it is ConnectException -> {
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
        }.onResult {
            Account.saveToken(context) // Save the current account to file
            FirebaseUtil.publishFCMToken(api) // Publish the current FCM token to the server (used to send notifications)

            goToHome(it.username, it.permission)
        }.useLoadingBar(this)
    }

    fun goToHome(username: String, permission: Char) {
        (activity as LoginCompleteListener).onLoginComplete(username, permission)
    }

    interface LoginCompleteListener {
        fun onLoginComplete(username: String, permission: Char)
    }


    companion object {
        val TAG = "Login"
    }
}