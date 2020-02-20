package it.cnr.oldmusa.fragments

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.leinardi.android.speeddial.SpeedDialActionItem
import it.cnr.oldmusa.Account.isAdmin
import it.cnr.oldmusa.DeleteSiteMutation
import it.cnr.oldmusa.R
import it.cnr.oldmusa.SiteDetailsQuery
import it.cnr.oldmusa.api.CacheModel
import it.cnr.oldmusa.util.AndroidUtil.linkToList
import it.cnr.oldmusa.util.AndroidUtil.useLoadingBar
import it.cnr.oldmusa.util.AsyncUtil.async
import it.cnr.oldmusa.util.GraphQlUtil.downloadImageSync
import it.cnr.oldmusa.util.GraphQlUtil.mutate
import it.cnr.oldmusa.util.GraphQlUtil.query
import it.cnr.oldmusa.util.GraphQlUtil.uploadImageSync
import it.cnr.oldmusa.util.None
import it.cnr.oldmusa.util.Optional
import it.cnr.oldmusa.util.Some
import kotlinx.android.synthetic.main.add_map.*
import kotlinx.android.synthetic.main.fragment_home.swipeContainer
import kotlinx.android.synthetic.main.fragment_site.*
import kotlinx.android.synthetic.main.fragment_site.view.*
import kotlinx.android.synthetic.main.remove_museum.*
import kotlin.math.max


class SiteFragment : Fragment(),
    SiteMapFragment.OnSensorSelectListener, SwipeRefreshLayout.OnRefreshListener {

    val args: SiteFragmentArgs by navArgs()

    val cacheModel: CacheModel by activityViewModels()

    lateinit var currentSite: SiteDetailsQuery.Site

    var siteMap: SiteMapFragment? = null

    var currentImageW: Int = 1
    var currentImageH: Int = 1
    var currentBitmap: Bitmap? = null

    data class SensorData(val handle: SiteDetailsQuery.Sensor) {
        override fun toString(): String {
            return handle.name() ?: handle.id().toString()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        setHasOptionsMenu(true)
        activity?.title = "Sito"

        val view = inflater.inflate(R.layout.fragment_site, container, false)
        view.sensorList.layoutManager = LinearLayoutManager(context!!).apply {
            orientation = LinearLayoutManager.VERTICAL
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // permission
        if (isAdmin) {
            speedDial.visibility = View.VISIBLE
            speedDial.addActionItem(
                SpeedDialActionItem.Builder(R.id.fab_add_sensor, R.drawable.ic_sensor)
                    .setLabel("Add Sensor")
                    .create()
            )
            speedDial.addActionItem(
                SpeedDialActionItem.Builder(R.id.fab_change_image, R.drawable.ic_map)
                    .setLabel("Change image")
                    .create()
            )

            speedDial.setOnActionSelectedListener {
                when (it.id) {
                    R.id.fab_add_sensor -> {
                        findNavController().navigate(SiteFragmentDirections.actionSiteToCreateSensor(args.siteId))
                        false
                    }
                    R.id.fab_change_image -> {
                        changeImage()
                        false
                    }
                    else -> true
                }
            }
        }

        // SwipeRefreshLayout
        swipeContainer.setOnRefreshListener(this)

        swipeContainer.post {
            swipeContainer.isRefreshing = true

            reloadSite()
        }

        swipeContainer.linkToList(sensorList)

        sensorList.addItemDecoration(DividerItemDecoration(context!!, DividerItemDecoration.VERTICAL))
    }

    fun changeImage() {
        val mBuilder = AlertDialog.Builder(context!!)
        mBuilder.setTitle("Aggiungi mappa")
        val d = mBuilder.setView(LayoutInflater.from(context!!).inflate(R.layout.add_map, null)).create()
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

    override fun onRefresh() {
        cacheModel.mapCache.remove(args.siteId)
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
                val dialog = mBuilder.setView(LayoutInflater.from(context!!).inflate(R.layout.remove_museum, null)).create()
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
                findNavController().navigate(
                    SiteFragmentDirections.actionSiteToCreateSite(
                        CreateSiteFragment.SiteDetails(
                            args.siteId,
                            currentSite.name(),
                            currentSite.idCnr()
                        )
                    )
                )
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun reloadSite() {
        val siteId = args.siteId

        query(SiteDetailsQuery(siteId)).onResult { data ->
            currentSite = data.site()

            val list = data.site()
                .sensors()
                .sortedBy { it.id() }
                .map { SensorData(it) }

            sensorList.adapter = MyRecyclerViewAdapter(currentSite.hasImage(), list)

            if (data.site().hasImage()) {
                currentImageW = data.site().imageWidth() ?: 1
                currentImageH = data.site().imageHeight() ?: 1

                setupMapFragment()
                loadImage()
            }


            activity?.title = currentSite.name() ?: ""
        }.onDone {
            swipeContainer.isRefreshing = false
        }
    }

    private fun loadImage() {
        // TODO: We might ask for the last time change from the server to build a safe-cache

        async {
            val cached = cacheModel.mapCache.get(args.siteId)

            if (cached != null) {
                return@async Optional.ofNullable(cached)
            }

            val res = downloadImageSync(requireContext(), args.siteId)
            Optional.ofNullable(res)
        }.onResult { mapData ->
            mapData.asNullable()?.let {
                cacheModel.mapCache.put(args.siteId, it)
            }
            onImageLoaded(mapData.asNullable())
        }

    }

    private fun onImageLoaded(bmp: Bitmap?) {
        if (bmp != null) {
            currentBitmap = bmp
            currentImageW = bmp.width
            currentImageH = bmp.height
        } else {
            currentBitmap = null
        }
        setupMapFragment()
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
                } else {
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
            val inStream = context!!.contentResolver.openInputStream(data.data!!)!!

            async {
                val bytes = inStream.readBytes()
                val opts = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)

                uploadImageSync(context!!, args.siteId, context!!.contentResolver.openInputStream(data.data!!)!!, opts.outWidth, opts.outHeight)
            }.onResult {
                cacheModel.mapCache.remove(args.siteId)
                reloadSite()
            }.useLoadingBar(this)
        }
    }

    fun setupMapFragment() {
        val siteMap = siteMap ?: return

        siteMap.sensorSelectListener = this
        val bmp = currentBitmap
        if (bmp != null) {
            siteMap.onRefresh(bmp, currentSite.sensors())
        } else {
            siteMap.setImageSize(currentImageW, currentImageH)
        }
    }

    inner class MyRecyclerViewAdapter(
        val hasImage: Boolean,
        val values: List<SensorData>
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): RecyclerView.ViewHolder {
            return when(viewType) {
                ADAPTER_TYPE_HEADER -> {
                    val view = if (hasImage) {
                        var smap = siteMap

                        if (smap == null || smap.view == null) {
                            layoutInflater.inflate(R.layout.list_sensor_header, parent, false)
                            smap =
                                childFragmentManager.findFragmentById(R.id.siteMap) as SiteMapFragment
                            siteMap = smap
                        }
                        setupMapFragment()
                        smap.view!!
                    } else {
                        layoutInflater.inflate(R.layout.site_map_empty, parent, false)
                    }
                    VHHeader(view)
                }
                ADAPTER_TYPE_ITEM -> VHItem(layoutInflater.inflate(R.layout.list_sensor_item, parent, false) as TextView)
                ADAPTER_TYPE_EMPTY -> {
                    val view = layoutInflater.inflate(R.layout.list_empty_item, parent, false)
                    view.findViewById<TextView>(R.id.textMain).text = getString(R.string.noSensorFound)
                    return VHHeader(view)
                }
                else -> throw RuntimeException("Unknown type: $viewType")
            }
        }

        override fun getItemCount(): Int {
            return max(1, values.size) + 1
        }

        override fun onBindViewHolder(
            holder: RecyclerView.ViewHolder,
            position: Int
        ) {
            if (holder is VHItem) {
                val realIndex = position - 1

                holder.view.text = values[realIndex].toString()

                holder.view.setOnClickListener {
                    val sensor = values[realIndex]

                    findNavController().navigate(
                        SiteFragmentDirections.actionSiteToChannel(
                            sensor.handle.id()
                        )
                    )
                }
            }
        }

        override fun getItemViewType(position: Int): Int {
            if (position == 0) return ADAPTER_TYPE_HEADER
            if (values.isEmpty()) return ADAPTER_TYPE_EMPTY
            return ADAPTER_TYPE_ITEM
        }

        fun destroy() {
        }

        inner class VHHeader(val view: View) : RecyclerView.ViewHolder(view)

        inner class VHItem(val view: TextView) : RecyclerView.ViewHolder(view)
    }



    companion object {
        // Adapter
        const val ADAPTER_TYPE_HEADER = 0
        const val ADAPTER_TYPE_ITEM = 1
        const val ADAPTER_TYPE_EMPTY = 2

        //image pick code
        private val IMAGE_PICK_CODE = 1000
        //Permission code
        private val PERMISSION_CODE = 1001
        private const val TAG = "Site"
    }
}
