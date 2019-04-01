package com.cnr_isac.oldmusa

import android.app.AlertDialog
import android.app.Dialog
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.add_museum.view.*
import android.view.ViewGroup
import android.view.WindowManager
import kotlinx.android.synthetic.main.add_museum.*


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

        addSiti.setOnClickListener{
            //val mDialogView = LayoutInflater.from(this).inflate(R.layout.add_museum, null)

            val mBuilder = AlertDialog.Builder(this)
            mBuilder.setTitle("Aggiungi museo")
            val d = mBuilder.setView(LayoutInflater.from(this).inflate(R.layout.add_museum, null)).create()
            val lp = WindowManager.LayoutParams()
            lp.copyFrom(d.window!!.attributes)
            lp.title = "Aggiungi museo"
            lp.width = (resources.displayMetrics.widthPixels * 0.80).toInt()
            lp.height = (resources.displayMetrics.heightPixels * 0.50).toInt()
            d.show()
            d.window!!.attributes = lp

            d.AddButtonM.setOnClickListener {
                d.dismiss()

            }
        }

    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (mToggle.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
