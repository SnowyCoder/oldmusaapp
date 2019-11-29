package it.cnr.oldmusa.fragments

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import it.cnr.oldmusa.*
import it.cnr.oldmusa.Account.isAdmin
import it.cnr.oldmusa.R.layout.*
import it.cnr.oldmusa.type.SensorInput
import it.cnr.oldmusa.type.SiteInput
import it.cnr.oldmusa.util.AndroidUtil.toNullableString
import it.cnr.oldmusa.util.GraphQlUtil.mutate
import it.cnr.oldmusa.util.GraphQlUtil.query
import it.cnr.oldmusa.util.AndroidUtil.useLoadingBar
import it.cnr.oldmusa.util.AsyncUtil.async
import it.cnr.oldmusa.util.GraphQlUtil.MapResizeData
import it.cnr.oldmusa.util.GraphQlUtil.downloadImageSync
import it.cnr.oldmusa.util.GraphQlUtil.uploadImageSync
import kotlinx.android.synthetic.main.add_map.*
import kotlinx.android.synthetic.main.add_sensor.*
import kotlinx.android.synthetic.main.edit_museum.*
import kotlinx.android.synthetic.main.fragment_home.swipeContainer
import kotlinx.android.synthetic.main.fragment_site.*
import kotlinx.android.synthetic.main.remove_museum.*


class SiteFragment : Fragment(),
    SiteMapFragment.OnSensorSelectListener, SwipeRefreshLayout.OnRefreshListener {


    val args: SiteFragmentArgs by navArgs()

    lateinit var currentSite: SiteDetailsQuery.Site

    var currentImageW: Int = 0
    var currentImageH: Int = 0

    data class SensorData(val handle: SiteDetailsQuery.Sensor) {
        override fun toString(): String {
            return handle.name() ?: handle.id().toString()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        setHasOptionsMenu(true)
        activity?.title = "Sito"

        val view = inflater.inflate(fragment_site, container, false)


        (childFragmentManager.findFragmentById(R.id.siteMap)!! as SiteMapFragment).sensorSelectListener = this

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // permission
        if (isAdmin) {
            addMapbutton.visibility = View.VISIBLE

            addSensorbutton.visibility = View.VISIBLE
        }

        // add event listener to array items
        sensorList.onItemClickListener = AdapterView.OnItemClickListener { _, view, index, _ ->
            val sensor = sensorList.adapter.getItem(index) as SensorData

            view.findNavController().navigate(
                SiteFragmentDirections.actionSiteToChannel(
                    sensor.handle.id()
                )
            )
        }

        // open map options modal
        addMapbutton.setOnClickListener {
            val mBuilder = AlertDialog.Builder(context!!)
            mBuilder.setTitle("Aggiungi mappa")
            val d = mBuilder.setView(LayoutInflater.from(context!!).inflate(add_map, null)).create()
            val lp = WindowManager.LayoutParams()
            lp.copyFrom(d.window!!.attributes)
            lp.width = (resources.displayMetrics.widthPixels * 0.80).toInt()
            lp.height = (resources.displayMetrics.heightPixels * 0.35).toInt()
            d.show()
            d.window!!.attributes = lp


            d.imgPickButton.setOnClickListener {
                // check runtime permission
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                    if (context!!.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) ==
                        PackageManager.PERMISSION_DENIED){
                        // permission denied
                        val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                        // show popup to request runtime permission
                        requestPermissions(permissions,
                            PERMISSION_CODE
                        )
                    }
                    else{
                        // permission already granted
                        pickImageFromGallery()
                    }
                }
                else{
                    // system OS is < Marshmallow
                    pickImageFromGallery()
                }

            }
        }

        // open add sensor modal
        addSensorbutton.setOnClickListener {
            //val mDialogView = LayoutInflater.from(this).inflate(R.layout.add_museum, null)

            val mBuilder = AlertDialog.Builder(context!!)
            mBuilder.setTitle("Aggiungi sensore")
            val dialog = mBuilder.setView(LayoutInflater.from(context!!).inflate(add_sensor, null)).create()
            val lp = WindowManager.LayoutParams()
            lp.copyFrom(dialog.window!!.attributes)
            lp.title = "Aggiungi sensore"
            lp.width = (resources.displayMetrics.widthPixels * 0.80).toInt()
            lp.height = (resources.displayMetrics.heightPixels * 0.55).toInt()
            dialog.show()
            dialog.window!!.attributes = lp

            dialog.AddButtonS.setOnClickListener {
                val name = dialog.name
                val idCnr = dialog.idCnr
                val enabled = dialog.enabled

                mutate(
                    AddSensorMutation(
                        args.siteId,
                        SensorInput.builder()
                            .name(name.text.toNullableString())
                            .idCnr(idCnr.text.toNullableString())
                            .enabled(enabled.isChecked)
                            .build()
                    )
                ).onResult {
                    dialog.dismiss()
                    reloadSite()
                }.useLoadingBar(this)
            }
        }

        // SwipeRefreshLayout
        swipeContainer.setOnRefreshListener(this)

        swipeContainer.post {
            swipeContainer.isRefreshing = true

            reloadSite()
        }
    }

    override fun onRefresh() {
        reloadSite()
    }

    override fun onSensorSelect(sensorId: Int) {
        view!!.findNavController().navigate(
            SiteFragmentDirections.actionSiteToChannel(
                sensorId
            )
        )
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (isAdmin) {
            inflater.inflate(R.menu.overflow_menu, menu)
            super.onCreateOptionsMenu(menu, inflater)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.remove -> {
                val mBuilder = AlertDialog.Builder(context!!)
                val dialog = mBuilder.setView(LayoutInflater.from(context!!).inflate(remove_museum, null)).create()
                val lp = WindowManager.LayoutParams()
                lp.copyFrom(dialog.window!!.attributes)
                lp.width = (resources.displayMetrics.widthPixels * 0.75).toInt()
                lp.height = (resources.displayMetrics.heightPixels * 0.30).toInt()
                dialog.show()
                dialog.window!!.attributes = lp

                dialog.ButtonYes.setOnClickListener {
                    mutate(DeleteSiteMutation(args.siteId)).onResult {
                        dialog.dismiss()
                        this.activity!!.onBackPressed()
                    }
                }
                dialog.ButtonNo.setOnClickListener {
                    dialog.dismiss()
                }

            }
            R.id.edit -> {
                val mBuilder = AlertDialog.Builder(context!!)
                mBuilder.setTitle("Modifica il museo")
                val dialog = mBuilder.setView(LayoutInflater.from(context!!).inflate(edit_museum, null)).create()
                val lp = WindowManager.LayoutParams()
                lp.copyFrom(dialog.window!!.attributes)
                lp.title = "modifica il museo"
                lp.width = (resources.displayMetrics.widthPixels * 0.80).toInt()
                lp.height = (resources.displayMetrics.heightPixels * 0.50).toInt()
                dialog.show()
                dialog.window!!.attributes = lp

                val nameSite = dialog.nameSite
                val idCnrSite = dialog.idCnrSite

                nameSite.setText(currentSite.name() ?: "")
                idCnrSite.setText(currentSite.idCnr() ?: "")

                dialog.Aggiorna.setOnClickListener {
                    mutate(
                        UpdateSiteMutation(
                            args.siteId,
                            SiteInput.builder()
                                .name(nameSite.text.toNullableString())
                                .idCnr(idCnrSite.text.toNullableString())
                                .build()
                        )
                    ).onResult {
                        dialog.dismiss()
                        reloadSite()
                    }.useLoadingBar(this)
                }

            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun reloadSite() {
        val siteId = args.siteId

        query(SiteDetailsQuery(siteId)).onResult { data ->
            currentSite = data.site()

            val list = data.site().sensors().map { SensorData(it) }

            val adapter = ArrayAdapter<SensorData>(context!!, list_sensor_item, list)
            sensorList.adapter = adapter
            noSensorText.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            activity?.title = currentSite.name() ?: ""

            swipeContainer.isRefreshing = false

            async {
                downloadImageSync(requireContext(), args.siteId)
            }.onResult { mapData ->
                if (mapData != null) {
                    currentImageW = mapData.width
                    currentImageH = mapData.height
                    (childFragmentManager.findFragmentById(R.id.siteMap)!! as SiteMapFragment).onRefresh(
                        mapData,
                        currentSite.sensors()
                    )
                    noMapText.visibility = View.INVISIBLE
                    noMapImage.visibility = View.INVISIBLE
                } else {
                    noMapText.visibility = View.VISIBLE
                    noMapImage.visibility = View.VISIBLE
                }
            }//.useLoadingBar(this)
            // TODO: think
            // Should we add a loading bar?
            // We might ask for the last time change from the server to build a safe-cache
        }
    }


    private fun pickImageFromGallery() {
        //Intent to pick image
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent,
            IMAGE_PICK_CODE
        )
    }

    //handle requested permission result
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when(requestCode){
            PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    //permission from popup granted
                    pickImageFromGallery()
                }
                else{
                    //permission from popup denied
                    Toast.makeText(context!!, "Permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    //handle result of picked image
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (data == null) return

        if (resultCode == Activity.RESULT_OK && requestCode == IMAGE_PICK_CODE){
            val bytes = context!!.contentResolver.openInputStream(data.data!!)!!.readBytes()

            var resize: MapResizeData? = null

            if (currentImageW != 0) {
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                resize = MapResizeData(currentImageW, currentImageH, bitmap.width, bitmap.height)
            }

            async {
                uploadImageSync(context!!, args.siteId, context!!.contentResolver.openInputStream(data.data!!)!!, resize)
            }.onResult {
                reloadSite()
            }.useLoadingBar(this)
        }
    }

    companion object {
        //image pick code
        private val IMAGE_PICK_CODE = 1000
        //Permission code
        private val PERMISSION_CODE = 1001
        private const val TAG = "Site"
    }
}
