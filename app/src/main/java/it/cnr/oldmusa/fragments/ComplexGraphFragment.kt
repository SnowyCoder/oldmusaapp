package it.cnr.oldmusa.fragments


import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.*
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.content.PermissionChecker.PERMISSION_GRANTED
import androidx.core.content.PermissionChecker.checkSelfPermission
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate.rgb
import it.cnr.oldmusa.ComplexChannelReadingsQuery
import it.cnr.oldmusa.ComplexChannelReadingsQuery.Channel
import it.cnr.oldmusa.R
import it.cnr.oldmusa.util.AndroidUtil.useLoadingBar
import it.cnr.oldmusa.util.AsyncUtil.async
import it.cnr.oldmusa.util.GraphQlUtil.query
import kotlinx.android.synthetic.main.fragment_complex_graph.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class ComplexGraphFragment : Fragment() {

    val args: ComplexGraphFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        setHasOptionsMenu(true)
        activity?.title = "Graph"

        return inflater.inflate(R.layout.fragment_complex_graph, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        chart.apply {
            setBackgroundColor(Color.WHITE)
            setTouchEnabled(true)
            setDrawGridBackground(true)
        }

        loadData(args.channelsId, Date(args.startDate), Date(args.endDate))
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.sensor_graph_overflow_menu, menu)
        return super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.share -> {
                shareChart()
            }
            R.id.save -> {
                exportChart()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun exportChart() {
        if (checkSelfPermission(context!!, WRITE_EXTERNAL_STORAGE) != PERMISSION_GRANTED) {
            requestPermissions(arrayOf(WRITE_EXTERNAL_STORAGE), PERMISSION_CODE_EXPORT)
            return
        }

        async {
            val oldmusaDir = Environment.getExternalStorageDirectory().resolve("oldmusa")
            oldmusaDir.mkdirs()

            val filename = "${System.currentTimeMillis()}.png"
            val file = File(oldmusaDir, filename)
            saveChart(file)
            file
        }
            .useLoadingBar(this)
            .onResult {
                Toast.makeText(context!!, "Image saved as ${it.name}", Toast.LENGTH_SHORT).show()
            }
    }

    fun shareChart() {
        if (checkSelfPermission(context!!, WRITE_EXTERNAL_STORAGE) != PERMISSION_GRANTED) {
            requestPermissions(arrayOf(WRITE_EXTERNAL_STORAGE), PERMISSION_CODE_SHARE)
            return
        }

        async {
            saveChartCache()
        }
            .useLoadingBar(this)
            .onResult { uri ->
                val shareIntent: Intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    type = "image/png"
                }
                startActivity(Intent.createChooser(shareIntent, "Chart"))
            }
    }

    fun saveChartCache(): Uri {
        val folder = activity!!.cacheDir.resolve("images")
        folder.mkdirs()
        val file = folder.resolve("shared_image.png")

        saveChart(file)

        return FileProvider.getUriForFile(context!!, "it.cnr.oldmusa.fileprovider", file)
    }

    fun saveChart(file: File) {
        val image = chart.chartBitmap
        val os = FileOutputStream(file)

        image.compress(Bitmap.CompressFormat.PNG, 90, os)

        os.close()
    }

    fun setupGraph(data: List<Channel>, startDate: Calendar) {
        val measureUnits = data.mapNotNull { it.measureUnit() }
            .distinct()

        val sets = data.mapIndexed { index, channel ->
            val entries = channel.readings().map { entry ->
                // Compress coordinates (from the 1970 to the beginning of the day
                // it should fit nicely in a float (with more precision, I hope)
                val diff = entry.date().time - startDate.timeInMillis

                Entry(diff.toFloat(), entry.valueMin().toFloat(), entry)
            }
            val dataSet = LineDataSet(entries, channel.name())

            val color = LINE_COLORS[index % LINE_COLORS.size]

            dataSet.color = color
            dataSet.setCircleColor(color)

            if (measureUnits.size == 2) {
                dataSet.axisDependency = if (channel.measureUnit() == measureUnits[0]) {
                    YAxis.AxisDependency.LEFT
                } else {
                    YAxis.AxisDependency.RIGHT
                }
            }

            dataSet
        }

        chart.apply {
            xAxis.apply {
                isEnabled = true
                //enableGridDashedLine(10f, 10f, 0f)
                valueFormatter = CustomTimeFormatter(startDate)
                setLabelCount(7, false)
            }

            axisLeft.apply {
                enableGridDashedLine(10f, 10f, 0f)
                isEnabled = true
                if (measureUnits.size <= 2) {
                    valueFormatter = CustomMeasureFormatter(valueFormatter, measureUnits[0])
                }
            }

            axisRight.apply {
                if (measureUnits.size == 2) {
                    isEnabled = true
                    valueFormatter = CustomMeasureFormatter(valueFormatter, measureUnits[1])
                } else {
                    isEnabled = false
                }
            }

            description.apply {
                isEnabled = false
            }

            this.data = LineData(sets)
            this.invalidate()
        }
    }


    fun loadData(channelsId: IntArray, begin: Date, end: Date) {
        query(ComplexChannelReadingsQuery(channelsId.asList(), begin, end)).onResult {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = begin.time
            setupGraph(it.channels(), calendar)
        }.useLoadingBar(this)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_CODE_EXPORT &&
            checkSelfPermission(context!!, WRITE_EXTERNAL_STORAGE) == PERMISSION_GRANTED) {
            exportChart()
        } else if (requestCode == PERMISSION_CODE_SHARE&&
            checkSelfPermission(context!!, WRITE_EXTERNAL_STORAGE) == PERMISSION_GRANTED) {
            shareChart()
        }
    }

    class CustomTimeFormatter(val referenceDate: Calendar) : ValueFormatter() {
        private val formatter = SimpleDateFormat.getTimeInstance()

        override fun getFormattedValue(value: Float): String {
            // Value saved as milliseconds from the start of the day
            val millis = value.toLong()
            return formatter.format(Date(referenceDate.timeInMillis + millis))
        }

        override fun getAxisLabel(value: Float, axis: AxisBase): String {
            val cal = Calendar.getInstance()
            cal.timeInMillis = referenceDate.timeInMillis + value.toLong()

            val last = axis.mEntries[axis.mEntryCount - 1]
            val first = axis.mEntries[0]

            val interval = last - first

            return when {
                interval < TimeUnit.MINUTES.toMillis(1) -> {
                    "${cal.get(Calendar.SECOND).pad2()}.${cal.get(Calendar.MILLISECOND).pad2()}"
                }
                interval < TimeUnit.DAYS.toMillis(1) -> {
                    "${cal.get(Calendar.HOUR).pad2()}:${cal.get(Calendar.MINUTE).pad2()}"
                }
                interval < TimeUnit.DAYS.toMillis(31) -> {
                    "${cal.get(Calendar.DAY_OF_MONTH).pad2()}T${cal.get(Calendar.HOUR).pad2()}"
                }
                interval < TimeUnit.DAYS.toMillis(365) -> {
                    "${(cal.get(Calendar.MONTH) + 1).pad2()}/${cal.get(Calendar.DAY_OF_MONTH).pad2()}"
                }
                else -> {
                    "${cal.get(Calendar.YEAR)}/${(cal.get(Calendar.MONTH) + 1).pad2()}/${cal.get(Calendar.DAY_OF_MONTH).pad2()}"
                }
            }
        }
    }

    class CustomMeasureFormatter(val formatter: ValueFormatter, val measureName: String) : ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            return formatter.getFormattedValue(value) + " " + measureName
        }
    }

    companion object {
        val LINE_COLORS_1 = listOf(
            rgb("#396ab1"), rgb("#da7c30"), rgb("#3e9651"), rgb("#cc2529"),
            rgb("#535154"), rgb("#6b4c9a"), rgb("#922428"), rgb("#948b3d")
        )
        val LINE_COLORS = listOf(
            rgb("#ff5722"), rgb("#03a9f4"), rgb("#9c27b0"), rgb("#4caf50"),
            rgb("#3f51b5"), rgb("#e91e63"), rgb("#f44336"), rgb("#009688")
        )

        // Others from ColorTemplate: JOYFUL_COLORS, COLORFUL_COLORS, MATERIAL_COLORS

        const val PERMISSION_CODE_EXPORT = 1
        const val PERMISSION_CODE_SHARE = 2
    }
}

private fun Int.pad2(): String {
    return toString().padStart(2, '0')
}
