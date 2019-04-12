package com.cnr_isac.oldmusa

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.support.v7.app.AlertDialog
import android.view.View
import android.widget.EditText
import com.cnr_isac.oldmusa.api.rest.RestApi
import android.os.StrictMode



class Login : AppCompatActivity() {

    companion object {
        private const val url = "http://51.77.151.174:8081/api/"// Server URL
        val api = RestApi.httpRest(url)// Connection type (rest over http)
        var isAdmin = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
    }

    fun login(view: View) {
        val name = findViewById<EditText>(R.id.user)
        val pass = findViewById<EditText>(R.id.pass)

        try {
            api.login(name.text.toString(), pass.text.toString())

            if (api.getMe().permission == 'A') {
                isAdmin = true
            }

            val intent = Intent(this, Home::class.java)
            startActivity(intent)
            finish()

        } catch (E: Exception) {

            val dialogBuilder = AlertDialog.Builder(this)
            dialogBuilder.setMessage("Username o Password errati")
                .setPositiveButton("OK") { dialogInterface, _ ->
                    dialogInterface.cancel()
                }
            val alert = dialogBuilder.create()
            alert.setTitle("Error")
            alert.show()
        }
    }
}