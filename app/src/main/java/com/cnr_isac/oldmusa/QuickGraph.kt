package com.cnr_isac.oldmusa

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.cnr_isac.oldmusa.api.Channel
import com.cnr_isac.oldmusa.api.ChannelReading
import com.cnr_isac.oldmusa.api.Sensor
import com.cnr_isac.oldmusa.util.ApiUtil.api
import com.cnr_isac.oldmusa.util.ApiUtil.query
import com.cnr_isac.oldmusa.util.ApiUtil.withLoading
import com.cnr_isac.oldmusa.util.TimeUtil.midnightOf
import com.cnr_isac.oldmusa.util.TimeUtil.copy
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.SimpleDateFormat
import java.util.*


class QuickGraph : Fragment() {

    val args: QuickGraphArgs by navArgs()

    lateinit var chart: LineChart
    lateinit var data: LineData
    private lateinit var currentDate: Calendar

    private lateinit var currentSensor: Sensor


    // TODO: please pick better colors (Material ones for example)
    val colors = listOf(
        Color.RED, Color.BLUE, Color.GREEN, Color.MAGENTA, Color.CYAN, Color.GRAY
    )



    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        val view = inflater.inflate(R.layout.fragment_quickgraph, container, false)

        view.findViewById<Button>(R.id.change_date).setOnClickListener {
            val datePicker = DatePickerDialog(context!!, { _, year, month, dayOfMonth ->
                onDateChange(midnightOf(year, month, dayOfMonth))
            }, currentDate.get(Calendar.YEAR), currentDate.get(Calendar.MONTH), currentDate.get(Calendar.DAY_OF_MONTH))
            datePicker.show()
        }

        chart = view.findViewById<LineChart>(R.id.chart).apply {
            setBackgroundColor(Color.WHITE)
            description.isEnabled = true
            setTouchEnabled(true)
            //setOnChartValueSelectedListener(this)
            setDrawGridBackground(true)
        }

        val xAxis = chart.xAxis
        xAxis.enableGridDashedLine(10f, 10f, 0f)
        xAxis.valueFormatter = object : ValueFormatter() {

            private val formatter = SimpleDateFormat.getTimeInstance()

            override fun getFormattedValue(value: Float): String {
                // Value saved as milliseconds from the start of the day
                val millis = value.toLong()
                return formatter.format(Date(currentDate.timeInMillis + millis))
            }
        }

        val yAxis = chart.axisLeft
        // disable dual axis (only use LEFT axis)
        chart.axisRight.isEnabled = false

        // horizontal grid lines
        yAxis.enableGridDashedLine(10f, 10f, 0f)


        onSensorLoad()

        /*var x = 0.0f
        val numDataPoints : Int = 1000

        val sinVals = ArrayList<Entry>()
        val cosVals = ArrayList<Entry>()


        for (i in 0 until numDataPoints) {
            val sinFunction = Math.sin(x.toDouble()).toFloat()
            val cosFunction = Math.cos(x.toDouble()).toFloat()
            x += 0.1f
            sinVals.add(Entry(x, sinFunction))
            cosVals.add(Entry(x, cosFunction))
            //xAXES.add(i, x.toString())
        }

        val sinDataSet = LineDataSet(sinVals, "DataSet 1")
        sinDataSet.color = Color.RED
        sinDataSet.setCircleColor(Color.RED)

        val cosDataSet = LineDataSet(cosVals, "DataSet 2")
        cosDataSet.color = Color.BLUE
        cosDataSet.setCircleColor(Color.BLUE)

        chart.data = LineData(sinDataSet, cosDataSet)
        */
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        onSensorLoad()
    }

    fun onSensorLoad() {
        // Setup first date
        currentDate = midnightOf(2014, Calendar.APRIL, 1)// TODO: replace with LocalDate.now(), this is for testing purposes
        onDateChange(currentDate)
    }


    fun onDateChange(day: Calendar) {
        val sensorId = args.sensorId
        val from = day
        val to = from.copy()
        to.add(Calendar.DAY_OF_MONTH, 1)


        query {
            currentSensor = api.getSensor(sensorId)
            currentSensor.channels.map {
                Pair(it, it.getReadings(from.time, to.time))
            }
        }.onResult {
            Log.d(TAG, "Data: ${userFriendlyDateFormatter.format(day.time)}")
            onDataReceived(day, it)
        }.withLoading(this)
    }

    fun onDataReceived(day: Calendar, data: List<Pair<Channel, List<ChannelReading>>>) {
        currentDate = day

        view!!.findViewById<TextView>(R.id.date_text).text = getString(R.string.current_date).format(userFriendlyDateFormatter.format(day.time))

        val datasets = data.mapIndexed { index, (channel, readings) -> createData(channel, readings, day, index) }

        chart.data = LineData(datasets)
        chart.invalidate()// Refresh
        Log.i(TAG, "Data reloaded: $datasets")
    }

    fun createData(channel: Channel, data: List<ChannelReading>, start: Calendar, index: Int): LineDataSet {
        val color = colors[index % colors.size]

        val vals = data.map {
            // Compress coordinates (from the 1970 to the beginning of the day
            // it should fit nicely in a float (with more precision, I hope)
            val diff = it.date.time - start.timeInMillis

            Entry(diff.toFloat(), it.valueMin.toFloat())
        }


        val dataSet = LineDataSet(vals, channel.name)
        dataSet.color = color
        dataSet.setCircleColor(color)
        return dataSet
    }

    companion object {
        private const val TAG = "QuickGraph"
        private val userFriendlyDateFormatter = SimpleDateFormat.getDateInstance()
    }
}