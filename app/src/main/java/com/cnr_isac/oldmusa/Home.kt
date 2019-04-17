package com.cnr_isac.oldmusa

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.*
import com.cnr_isac.oldmusa.Account.api
import com.cnr_isac.oldmusa.api.Site
import com.cnr_isac.oldmusa.util.ApiUtil.query
import com.cnr_isac.oldmusa.util.ApiUtil.withLoading
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.add_museum.*


class Home : AppCompatActivity() {

    lateinit var mDrawerlayout: DrawerLayout
        private set

    lateinit var mToggle: ActionBarDrawerToggle
        private set

    lateinit var sites: List<Site>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val listView = findViewById<ListView>(R.id.ListMuseum)

        // permission
        if (api.getMe().isAdmin)
        {
            val buttonVisible = findViewById<ImageButton>(R.id.addSiti)
            buttonVisible.visibility=View.VISIBLE
        }

        query {
            api.getSites()
        }.onResult { sites ->
            this.sites = sites

            val nameList = sites.map { it.name ?: "null" }

            Log.e(TAG, nameList.toString())

            val adapter = ArrayAdapter<String>(this, R.layout.list_museum_item, nameList)
            listView.adapter = adapter
        }.withLoading(this)

        listView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val intent = Intent(this, Museum::class.java)
            intent.putExtra("site", sites[position].id)
            startActivity(intent)
            finish()
        }

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

            d.AddButtonM.setOnClickListener { view ->
                d.dismiss()
                addMuseum(view)
            }
        }

    }

    fun addMuseum (view: View) {
        Log.e("start", "start add museum")
        val museumName = view.findViewById<EditText>(R.id.nameMuseum)!!
        val idCnr = view.findViewById<EditText>(R.id.idCnr)!!
        query {
            val newMuseum = api.addSite()
            newMuseum.name = museumName.text.toString()
            Log.e("text", newMuseum.name)
            newMuseum.idCnr = idCnr.text.toString()
            Log.e("text", newMuseum.idCnr)
            newMuseum.commit()
        }.withLoading(this)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (mToggle.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        const val TAG = "Home"
    }
}
