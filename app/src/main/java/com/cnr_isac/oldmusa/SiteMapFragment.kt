package com.cnr_isac.oldmusa

import android.graphics.Bitmap
import android.os.Bundle
import android.util.TypedValue
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.math.MathUtils.clamp
import androidx.fragment.app.Fragment
import com.cnr_isac.oldmusa.api.Sensor
import com.cnr_isac.oldmusa.util.ApiUtil.isAdmin
import com.cnr_isac.oldmusa.util.ApiUtil.query
import kotlin.math.roundToInt


class SiteMapFragment : Fragment() {
    lateinit var mapContainer: ViewGroup
    lateinit var mapImageView: ImageView

    lateinit var sensors: List<Sensor>
    var mapWidth: Int = 0
    var mapHeight: Int = 0

    var sensorSelectListener: OnSensorSelectListener? = null

    var currentMovingSensor: Sensor? = null
    var moveXDelta: Int = 0
    var moveYDelta: Int = 0

    var imageSizePixels: Int = -1


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.site_map, container, false)

        mapContainer = view.findViewById(R.id.mapContainer)
        mapImageView = view.findViewById(R.id.mapMuseum)

        imageSizePixels = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20f, resources.displayMetrics).toInt()

        return view
    }

    fun onRefresh(map: Bitmap, sensors: List<Sensor>) {
        // Cleanup
        for (i in (1 until mapContainer.childCount).reversed()) {
            this.mapContainer.removeViewAt(i)
        }

        this.sensors = sensors

        mapImageView.setImageBitmap(map)

        mapWidth = map.width
        mapHeight = map.height

        sensors.forEachIndexed { index, sensor ->
            addSensorState(mapContainer, sensor, index)
        }

        mapContainer.invalidate()
    }

    fun addSensorState(parent: ViewGroup, sensor: Sensor, index: Int) {
        val locX = sensor.locX?.toInt()
        val locY = sensor.locY?.toInt()

        if (locX == null || locY == null) return

        val sensorListener = SensorListener(index, mapContainer)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        if (userVisibleHint) {
            when(item.itemId) {
                R.id.open -> sensorSelectListener?.onSensorSelect(sensors[item.groupId].id)
                R.id.move -> {
                    currentMovingSensor = sensors[item.groupId]
                    Toast.makeText(context!!, "You can now move the sensor", Toast.LENGTH_LONG).show()
                }
            }
            return true
        }
        /*isAdmin {
            if (!it) return@isAdmin



            /*val buttonVisible3 = findViewById<ImageButton>(R.id.addChannelButton)
            buttonVisible3.visibility=View.VISIBLE*/
        }*/
        return false
    }


    inner class SensorListener(val index: Int, parent: ViewGroup) : View.OnClickListener,
        View.OnCreateContextMenuListener, View.OnTouchListener {

        val sensor = sensors[index]

        val image: ImageView
        val name: TextView

        init {
            // TODO: draw real images (those are placeholders)
            image = ImageView(context!!)
            name = TextView(context!!)

            initViews(parent)
        }

        private fun initViews(parent: ViewGroup) {
            image.tag = index
            name.tag = index

            image.setImageResource(when {
                !sensor.enabled         -> R.drawable.ic_led_disabled
                sensor.status == "ok"   -> R.drawable.ic_led_ok
                else                    -> R.drawable.ic_led_error
            })

            name.text = sensor.name

            resetViewPosition()

            image.setOnClickListener(this)
            image.setOnCreateContextMenuListener(this)
            image.setOnTouchListener(this)

            parent.addView(image)
            parent.addView(name)
        }

        override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
            //if (currentMovingSensor != null) return

            menu.add(index, R.id.open, 0, "Open")
            menu.add(index, R.id.move, 1, "Move")
        }

        override fun onTouch(v: View, event: MotionEvent): Boolean {
            if (currentMovingSensor === sensor) {
                val rawX = event.rawX.toInt()
                val rawY = event.rawY.toInt()
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        val params = v.layoutParams as ViewGroup.MarginLayoutParams
                        moveXDelta = params.leftMargin - rawX
                        moveYDelta = params.topMargin - rawY
                    }
                    MotionEvent.ACTION_MOVE -> {
                        // Get values on the top-left of the image layout
                        // (we get those because the users move the sensor image and not his name)
                        // the deltas are initialized when the users begins moving the sensor, they represent how off the
                        // user touch is off from the top-left of the layout, if the user moves the sensor clicking on
                        // it's centre the deltas will then be te same as half of the image width and height.
                        changeViewPosition(rawX + moveXDelta, rawY + moveYDelta)
                    }
                    MotionEvent.ACTION_UP -> {
                        AlertDialog.Builder(context!!)
                            .setTitle("Confirm")
                            .setMessage("Do you want to save this position?")
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setPositiveButton(android.R.string.yes) { dialog, whichButton ->
                                savePosition()
                                currentMovingSensor = null
                            }
                            .setNegativeButton(android.R.string.no) { dialog, whichButton ->
                                //reset position
                                val locX = clamp(sensor.locX!!.toInt(), 0, mapWidth)
                                val locY = clamp(sensor.locY!!.toInt(), 0, mapHeight)

                                var padLeft = ((locX.toDouble() / mapWidth) * mapContainer.width).roundToInt()
                                val padBottom = ((locY.toDouble() / mapHeight) * mapContainer.height).roundToInt()
                                var padTop = mapContainer.height - padBottom

                                padLeft = clamp(padLeft, 0, mapContainer.width - imageSizePixels)
                                padTop = clamp(padTop, 0, mapContainer.height - imageSizePixels)
                                changeViewPosition(padLeft, padTop)

                                currentMovingSensor = null
                                Toast.makeText(context!!, "Moving cancelled", Toast.LENGTH_SHORT).show()
                            }
                            .setNeutralButton("Continue", null)
                            .setCancelable(false)
                            .show()
                    }
                }
                return true
            }
            // Block touches if the user is moving a sensor
            return currentMovingSensor != null
        }

        override fun onClick(v: View) {
            sensorSelectListener?.onSensorSelect(sensor.id)
        }

        fun savePosition() {
            val params = image.layoutParams as ViewGroup.MarginLayoutParams
            val padLeft = params.leftMargin
            val padTop = params.topMargin
            val padBottom = mapContainer.height - padTop

            val locX = ((padLeft.toDouble() / mapContainer.width) * mapWidth).toLong()
            val locY = ((padBottom.toDouble() / mapContainer.height) * mapHeight).toLong()

            query {
                sensor.locX = locX
                sensor.locY = locY
                sensor.commit()
            }.onResult {
                Toast.makeText(context!!, "Position saved", Toast.LENGTH_SHORT).show()
            }
        }

        fun changeViewPosition(layoutX: Int, layoutY: Int) {
            // Compute the layout center
            if (name.measuredHeight == 0) {
                name.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
            }
            val imageXCenter = layoutX + imageSizePixels / 2

            val imageParams = image.layoutParams as ViewGroup.MarginLayoutParams
            imageParams.setMargins(layoutX, layoutY, 0, 0)
            image.layoutParams = imageParams

            // Center the text on top of the image

            val nameParams = name.layoutParams as ViewGroup.MarginLayoutParams
            nameParams.setMargins(imageXCenter - name.measuredWidth / 2, layoutY - name.measuredHeight, 0, 0)
            name.layoutParams = nameParams
        }

        fun resetViewPosition() {
            val locX = clamp(sensor.locX!!.toInt(), 0, mapWidth)
            val locY = clamp(sensor.locY!!.toInt(), 0, mapHeight)

            var padLeft = ((locX.toDouble() / mapWidth) * mapContainer.width).roundToInt()
            val padBottom = ((locY.toDouble() / mapHeight) * mapContainer.height).roundToInt()
            var padTop = mapContainer.height - padBottom

            padLeft = clamp(padLeft, 0, mapContainer.width - imageSizePixels)
            padTop = clamp(padTop, 0, mapContainer.height - imageSizePixels)


            image.layoutParams = ViewGroup.MarginLayoutParams(imageSizePixels, imageSizePixels)
            name.layoutParams = ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)

            changeViewPosition(padLeft, padTop)
        }
    }


    interface OnSensorSelectListener {
        fun onSensorSelect(sensorId: Long)
    }
}