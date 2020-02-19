package it.cnr.oldmusa.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.apollographql.apollo.exception.ApolloNetworkException
import it.cnr.oldmusa.LoginMutation
import it.cnr.oldmusa.R
import it.cnr.oldmusa.StartupQuery
import it.cnr.oldmusa.api.graphql.LoginRequiredGraphQlException
import it.cnr.oldmusa.api.graphql.WrongPasswordGraphQlException
import it.cnr.oldmusa.firebase.FirebaseUtil
import it.cnr.oldmusa.util.AndroidUtil.useLoadingBar
import it.cnr.oldmusa.util.GraphQlUtil
import it.cnr.oldmusa.util.GraphQlUtil.apollo
import it.cnr.oldmusa.util.GraphQlUtil.apolloCall
import it.cnr.oldmusa.util.GraphQlUtil.asChar
import it.cnr.oldmusa.util.GraphQlUtil.handleError
import it.cnr.oldmusa.util.SemVer
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

        startInitialConnection()
    }

    fun startInitialConnection() {
        // If the api is already logged in, skip
        apolloCall(apollo.query(StartupQuery()), manageError = false)
            .onResult {
                if (!checkSemver(it.apiVersion())) {
                    return@onResult
                }

                val user = it.userMe()
                if (user != null) {
                    goToHome(user.id(), user.username(), user.permission().asChar())
                }
            }.onError {
                when {
                    it is WrongPasswordGraphQlException || it is LoginRequiredGraphQlException -> {
                        // ignore
                    }
                    manageConnectionException(it) -> { // already handled
                    }
                    else -> {
                        handleError(requireActivity(), it)
                    }
                }
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
                when {
                    it is WrongPasswordGraphQlException -> {
                        val dialogBuilder = AlertDialog.Builder(context)
                        dialogBuilder.setMessage("Username o Password errati")
                            .setPositiveButton("OK") { dialogInterface, _ ->
                                dialogInterface.cancel()
                            }
                        val alert = dialogBuilder.create()
                        alert.setTitle("Error")
                        alert.show()
                    }
                    manageConnectionException(it) -> { // already handled
                    }
                    else -> {
                        handleError(context, it)
                    }
                }
            }
            .onResult {
                FirebaseUtil.publishFCMToken(apollo) // Publish the current FCM token to the server (used to send notifications)

                val data = it.login()
                goToHome(data.id(), name.toString(), data.permission().asChar())
            }.useLoadingBar(this)
    }

    fun checkSemver(serverVersion: String): Boolean {
        val serverSemVer = SemVer.parseOrNull(serverVersion)

        if (serverSemVer == null) {
            val builder = AlertDialog.Builder(context)
            builder.setTitle("Server error")
            builder.setMessage("Invalid server version: $serverVersion\nCannot connect to server")
            builder.setNeutralButton("Change Server") { _, _ ->
                val action = LoginFragmentDirections.actionLoginToSettings(SettingsFragment.QuickEditNavs.EDIT_API_URL)
                findNavController().navigate(action)
            }
            builder.setCancelable(false)
            builder.show()
            return false
        }

        if (!GraphQlUtil.CURRENT_VERSION.isForwardsCompatibleTo(serverSemVer)) {
            val builder = AlertDialog.Builder(context)
            builder.setTitle("Server error")
            builder.setMessage("Server is not compatible with app version.\n app: ${GraphQlUtil.CURRENT_VERSION}, server: $serverSemVer")
            builder.setNeutralButton("Change Server") { _, _ ->
                val action = LoginFragmentDirections.actionLoginToSettings(SettingsFragment.QuickEditNavs.EDIT_API_URL)
                findNavController().navigate(action)
            }
            builder.setNeutralButton("Update App") { _, _ ->
                // TODO: open appstore or similar
                Toast.makeText(requireContext(), "Please update the app manually", Toast.LENGTH_LONG).show()
            }
            builder.setCancelable(false)
            builder.show()
            return false
        }

        return true
    }

    fun manageConnectionException(e: Exception): Boolean {
        return if (e is ConnectException || e is ApolloNetworkException) {
            val builder = AlertDialog.Builder(context)
            builder.setMessage("Errore nella connesione al server")

            builder.setPositiveButton("Retry") { _, _ ->
                startInitialConnection()
            }
            builder.setNeutralButton("Change server") { _, _ ->
                val action = LoginFragmentDirections.actionLoginToSettings(SettingsFragment.QuickEditNavs.EDIT_API_URL)
                findNavController().navigate(action)
            }
            builder.setTitle("Server Error")
            builder.setCancelable(false)
            builder.show()
            Log.e(TAG, "Error connecting to the server", e)
            true
        } else false
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