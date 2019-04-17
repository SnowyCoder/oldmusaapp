package com.cnr_isac.oldmusa

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.EditText
import com.cnr_isac.oldmusa.Account.api
import com.cnr_isac.oldmusa.firebase.FirebaseUtil
import com.cnr_isac.oldmusa.util.ApiUtil
import com.cnr_isac.oldmusa.util.ApiUtil.handleRestError
import com.cnr_isac.oldmusa.util.ApiUtil.query
import com.cnr_isac.oldmusa.util.ApiUtil.withLoading


class Login : AppCompatActivity() {
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
            Account.saveToken(applicationContext)
            return true
        }
        return false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // TODO: Remove, we can keep a strong policy if we use the query API
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        createNotificationChannel()

        // If the api is already logged in, skip
        query { isLoginNeeded() }.onResult {
            //if (!it) goToHome()
        }
    }

    fun login(view: View) {
        val name = findViewById<EditText>(R.id.user)
        val pass = findViewById<EditText>(R.id.pass)


        ApiUtil.RawQuery {
            api.login(name.text.toString(), pass.text.toString())
        }.onRestError {
            if (it.code == 401) {
                val dialogBuilder = AlertDialog.Builder(this)
                dialogBuilder.setMessage("Username o Password errati")
                    .setPositiveButton("OK") { dialogInterface, _ ->
                        dialogInterface.cancel()
                    }
                val alert = dialogBuilder.create()
                alert.setTitle("Error")
                alert.show()
            } else handleRestError(applicationContext, it)
        }.onResult {
            Account.saveToken(applicationContext) // Save the current account to file
            FirebaseUtil.publishFCMToken(api) // Publish the current FCM token to the server (used to send notifications)

            goToHome()
        }.withLoading(this)
    }

    fun goToHome() {
        val intent = Intent(this, Home::class.java)
        startActivity(intent)
        finish()
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
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}