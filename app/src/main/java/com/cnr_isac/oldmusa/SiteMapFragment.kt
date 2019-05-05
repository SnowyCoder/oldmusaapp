package com.cnr_isac.oldmusa

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.cnr_isac.oldmusa.api.Sensor
import kotlin.math.roundToInt
import android.content.DialogInterface
import android.util.TypedValue
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import com.cnr_isac.oldmusa.util.ApiUtil.query


class SiteMapFragment : Fragment() {
    lateinit var mapContainer: FrameLayout
    lateinit var mapImageView: ImageView

    lateinit var sensors: List<Sensor>
    var mapWidth: Int = 0
    var mapHeight: Int = 0

    var sensorSelectListener: OnSensorSelectListener? = null

    var currentMovingSensor: Sensor? = null
    var moveXDelta: Int = 0
    var moveYDelta: Int = 0


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.site_map, container, false)

        mapContainer = view.findViewById(R.id.mapContainer)
        mapImageView = view.findViewById(R.id.mapMuseum)

        return view
    }

    fun onRefresh(map: Bitmap, sensors: List<Sensor>) {
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

        // TODO: draw sensor name
        // TODO: draw real images (those are placeholders)
        val image = when(sensor.status) {
            "ok" -> R.drawable.ic_circle_button
            else -> R.drawable.ic_sad
        }

        val view = ImageView(context!!)
        view.setImageResource(image)
        resetSensorViewPosition(view, sensor)


        val sensorListener = SensorListener(sensor, index)
        view.setOnClickListener(sensorListener)
        view.setOnCreateContextMenuListener(sensorListener)
        view.setOnTouchListener(sensorListener)

        parent.addView(view)
    }

    fun resetSensorViewPosition(view: View, sensor: Sensor) {
        val locX = sensor.locX!!.toInt()
        val locY = sensor.locY!!.toInt()

        val padLeft = ((locX.toDouble() / mapWidth) * mapContainer.width).roundToInt()
        val padBottom = ((locY.toDouble() / mapHeight) * mapContainer.height).roundToInt()
        val padTop = mapContainer.height - padBottom

        val size = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20f, resources.displayMetrics).toInt()
        view.layoutParams = FrameLayout.LayoutParams(size, size).also {
            it.setMargins(padLeft, padTop, 0, 0)
        }
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
        return false
    }


    inner class SensorListener(val sensor: Sensor, val index: Int) : View.OnClickListener, View.OnCreateContextMenuListener,
        View.OnTouchListener {
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
                        val params = v.layoutParams as ViewGroup.MarginLayoutParams
                        params.leftMargin = rawX + moveXDelta
                        params.topMargin = rawY + moveYDelta
                        v.layoutParams = params
                    }
                    MotionEvent.ACTION_UP -> {
                        AlertDialog.Builder(context!!)
                            .setTitle("Confirm")
                            .setMessage("Do you want to save this position?")
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setPositiveButton(android.R.string.yes) { dialog, whichButton ->
                                saveSensorPosition(currentMovingSensor!!, v)
                                currentMovingSensor = null
                            }
                            .setNegativeButton(android.R.string.no) { dialog, whichButton ->
                                resetSensorViewPosition(v, currentMovingSensor!!)
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
    }

    fun saveSensorPosition(sensor: Sensor, sensorView: View) {
        val params = sensorView.layoutParams as ViewGroup.MarginLayoutParams
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


    interface OnSensorSelectListener {
        fun onSensorSelect(sensorId: Long)
    }
}