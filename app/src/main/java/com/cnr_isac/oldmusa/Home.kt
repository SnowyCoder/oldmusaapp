package com.cnr_isac.oldmusa

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.view.MenuItem

class Home : AppCompatActivity() {

    lateinit var mDrawerlayout: DrawerLayout
        private set

    lateinit var mToggle: ActionBarDrawerToggle
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        mDrawerlayout = findViewById(R.id.drawer)
        mToggle = ActionBarDrawerToggle(this, mDrawerlayout, R.string.open, R.string.close)
        mDrawerlayout.addDrawerListener(mToggle)
        mToggle.syncState()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (mToggle.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
