package com.cnr_isac.oldmusa


import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavOptions
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.cnr_isac.oldmusa.Account.isAdmin
import com.cnr_isac.oldmusa.fragments.LoginFragment
import com.cnr_isac.oldmusa.fragments.UserDetailsEditFragmentArgs
import com.cnr_isac.oldmusa.util.ApiUtil.api
import com.google.android.material.navigation.NavigationView
import kotlinx.android.synthetic.main.nav_header.view.*

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener,
    LoginFragment.LoginCompleteListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar: Toolbar = findViewById(R.id.toolbar)

        setSupportActionBar(toolbar)

        val navController = findNavController(R.id.nav_host_fragment)
        AppBarConfiguration(navController.graph)
        findViewById<NavigationView>(R.id.nav_view).setupWithNavController(navController)


        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Setup navigation view
        navView.setNavigationItemSelectedListener(this)
        setLoggedInButtonsVisibility(false)

        // TODO: Remove, we can keep a strong policy if we use the query API
        // Change Thread policy
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        // Manage notifications
        createNotificationChannel()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)

        when (item.itemId) {
            R.id.home -> {
                navController.popBackStack(R.id.home, false)
            }
            R.id.current_user_detail -> {
                navController.navigate(R.id.userDetailsEdit, UserDetailsEditFragmentArgs(api.getMe().id).toBundle())
            }
            R.id.manage_users -> {
                navController.navigate(R.id.manageUsers)
            }
            R.id.settings -> {
                navController.navigate(R.id.settings)
            }
            R.id.logout -> {
                api.logout()
                Account.saveToken(this)

                navController.navigate(R.id.login, null, NavOptions.Builder().setPopUpTo(R.id.home, true).build())
            }
            R.id.about -> {
                navController.navigate(R.id.about)
            }
        }

        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
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

    override fun onLoginComplete(username: String, permission: Char) {
        // Set navigation username
        val navView: NavigationView = findViewById(R.id.nav_view)
        setLoggedInButtonsVisibility(true)

        val usernameField: TextView = navView.getHeaderView(0).findViewById(R.id.username)
        usernameField.text = username

        Account.resetAdminCache(permission == 'A')


        navView.menu.findItem(R.id.manage_users).isVisible = isAdmin
        navView.menu.findItem(R.id.current_user_detail).isVisible = !isAdmin

        findNavController(R.id.nav_host_fragment).navigate(R.id.home, null, NavOptions.Builder().setPopUpTo(R.id.login, true).build())
    }

    private fun setLoggedInButtonsVisibility(value: Boolean) {
        val navView: NavigationView = findViewById(R.id.nav_view)
        navView.menu.findItem(R.id.home).isVisible = value
        navView.menu.findItem(R.id.current_user_detail).isVisible = value
        navView.menu.findItem(R.id.manage_users).isVisible = value

        val header = navView.getHeaderView(0)
        val visibility = if (value) View.VISIBLE else View.INVISIBLE
        header.photo.visibility = visibility
        header.username.visibility = visibility
    }
}
