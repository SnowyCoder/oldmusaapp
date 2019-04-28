package com.cnr_isac.oldmusa

import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.cnr_isac.oldmusa.util.ApiUtil.api
import com.cnr_isac.oldmusa.firebase.FirebaseUtil
import com.cnr_isac.oldmusa.util.ApiUtil
import com.cnr_isac.oldmusa.util.ApiUtil.handleRestError
import com.cnr_isac.oldmusa.util.ApiUtil.query
import com.cnr_isac.oldmusa.util.ApiUtil.withLoading


class Login : Fragment() {
    companion object {
        val TAG = "Login"
    }

    private fun isLoginNeeded(): Boolean {
        // TODO: we need a better solution to this
        if (api.getCurrentToken().isNullOrBlank()) return true
        try {
            api.getMe()
        } catch (e: RuntimeException) {
            api.logout()
            Account.saveToken(context!!)
            return true
        }
        return false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        val view = inflater.inflate(R.layout.fragment_login, container, false)

        // TODO: Remove, we can keep a strong policy if we use the query API
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        createNotificationChannel()

        // If the api is already logged in, skip
        query { isLoginNeeded() }.onResult {
            if (!it) goToHome()
        }

        return view
    }

    fun login(view: View) {
        val name = view.findViewById<EditText>(R.id.user)
        val pass = view.findViewById<EditText>(R.id.pass)


        ApiUtil.RawQuery {
            api.login(name.text.toString(), pass.text.toString())
        }.onRestError {
            if (it.code == 401) {
                val dialogBuilder = AlertDialog.Builder(context!!)
                dialogBuilder.setMessage("Username o Password errati")
                    .setPositiveButton("OK") { dialogInterface, _ ->
                        dialogInterface.cancel()
                    }
                val alert = dialogBuilder.create()
                alert.setTitle("Error")
                alert.show()
            } else handleRestError(context!!, it)
        }.onResult {
            Account.saveToken(context!!) // Save the current account to file
            FirebaseUtil.publishFCMToken(api) // Publish the current FCM token to the server (used to send notifications)

            goToHome()
        }.withLoading(this)
    }

    fun goToHome() {
        view!!.findNavController().navigate(LoginDirections.actionLoginToHome())
    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name)
            val descriptionText = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("alarm", name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager = context!!.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}