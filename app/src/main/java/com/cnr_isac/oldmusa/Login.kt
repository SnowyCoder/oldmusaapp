package com.cnr_isac.oldmusa

import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.cnr_isac.oldmusa.api.User
import com.cnr_isac.oldmusa.util.ApiUtil.api
import com.cnr_isac.oldmusa.firebase.FirebaseUtil
import com.cnr_isac.oldmusa.util.ApiUtil
import com.cnr_isac.oldmusa.util.ApiUtil.handleRestError
import com.cnr_isac.oldmusa.util.ApiUtil.query
import com.cnr_isac.oldmusa.util.ApiUtil.useLoadingBar
import kotlinx.android.synthetic.main.activity_login.*


class Login : AppCompatActivity() {
    companion object {
        val TAG = "Login"
    }

    /**
     * Checks if the login works by querying the current user
     * if the query fails it does not crash (instead it logs out)
     */
    private fun checkGetMe(): User? {
        // Try to get the current user
        if (api.getCurrentToken().isNullOrBlank()) return null
        return try {
            api.getMe()
        } catch (e: RuntimeException) {
            api.logout()
            Account.saveToken(this)
            null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_login)

        // TODO: Remove, we can keep a strong policy if we use the query API
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        createNotificationChannel()

        // If the api is already logged in, skip
        query { checkGetMe() }.onResult {
            if (it != null) goToHome(it.username)
        }
    }

    fun login(view: View) {
        val name = findViewById<EditText>(R.id.user).text
        val pass = findViewById<EditText>(R.id.pass).text

        if (name.isNullOrEmpty() or pass.isNullOrEmpty()) return



        ApiUtil.RawQuery {
            api.login(name.toString(), pass.toString())
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
            } else handleRestError(this, it)
        }.onResult {
            Account.saveToken(this) // Save the current account to file
            FirebaseUtil.publishFCMToken(api) // Publish the current FCM token to the server (used to send notifications)

            goToHome(name.toString())
        }.useLoadingBar(this)
    }

    fun goToHome(username: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("username", username)
        finish()
        startActivity(intent)
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
            val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}