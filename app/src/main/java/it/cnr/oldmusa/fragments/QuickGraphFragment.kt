package it.cnr.oldmusa.fragments

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.*
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import it.cnr.oldmusa.Account.isAdmin
import it.cnr.oldmusa.util.TimeUtil.copy
import it.cnr.oldmusa.util.TimeUtil.midnightOf
import it.cnr.oldmusa.util.TimeUtil.setMidnight
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import it.cnr.oldmusa.*
import it.cnr.oldmusa.type.ChannelInput
import it.cnr.oldmusa.util.GraphQlUtil.mutate
import it.cnr.oldmusa.util.AndroidUtil.useLoadingBar
import it.cnr.oldmusa.util.AndroidUtil.toNullableString
import it.cnr.oldmusa.util.GraphQlUtil.query
import kotlinx.android.synthetic.main.edit_channel.*
import kotlinx.android.synthetic.main.fragment_quickgraph.*
import kotlinx.android.synthetic.main.remove_channel.*
import java.text.SimpleDateFormat
import java.util.*


class QuickGraphFragment : Fragment(), SwipeRefreshLayout.OnRefreshListener {

    val args: QuickGraphFragmentArgs by navArgs()

    lateinit var data: LineData
    private lateinit var currentDate: Calendar

    private lateinit var currentChannel: ChannelDetailsQuery.Channel


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        setHasOptionsMenu(true)
        activity?.title = "Canale"

        return inflater.inflate(R.layout.fragment_quickgraph, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        changeDate.setOnClickListener {
            val theme = R.style.AlertDialogCustom
            val datePicker = DatePickerDialog(context!!, theme, { _, year, month, dayOfMonth ->
                onDateChange(midnightOf(year, month, dayOfMonth))
            }, currentDate.get(Calendar.YEAR), currentDate.get(Calendar.MONTH), currentDate.get(Calendar.DAY_OF_MONTH))
            datePicker.datePicker.layoutMode = 1// Add <21 compatibility
            datePicker.show()
        }

        chart.apply {
            setBackgroundColor(Color.WHITE)
            //description.isEnabled = true
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


        // SwipeRefreshLayout
        swipeContainer.setOnRefreshListener(this)

        swipeContainer.post {
            swipeContainer.isRefreshing = true

            onChannelLoad()
        }
    }

    override fun onRefresh() {
        onDateChange(currentDate)
    }

    fun onChannelLoad() {
        // Setup first date
        currentDate = Calendar.getInstance().setMidnight()
        onDateChange(currentDate)
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
                val dialog = mBuilder.setView(LayoutInflater.from(context!!).inflate(R.layout.remove_channel, null)).create()
                val lp = WindowManager.LayoutParams()
                lp.copyFrom(dialog.window!!.attributes)
                lp.width = (resources.displayMetrics.widthPixels * 0.75).toInt()
                lp.height = (resources.displayMetrics.heightPixels * 0.30).toInt()
                dialog.show()
                dialog.window!!.attributes = lp

                dialog.ButtonYesCha.setOnClickListener {
                    mutate (DeleteChannelMutation(args.channelId)).onResult {
                        dialog.dismiss()
                        activity!!.onBackPressed()
                    }.useLoadingBar(this)
                }
                dialog.ButtonNoCha.setOnClickListener {
                    dialog.dismiss()
                }
            }
            R.id.edit -> {
                val mBuilder = AlertDialog.Builder(context!!)
                mBuilder.setTitle("Modifica il canale")
                val dialog = mBuilder.setView(LayoutInflater.from(context!!).inflate(R.layout.edit_channel, null)).create()
                val lp = WindowManager.LayoutParams()
                lp.copyFrom(dialog.window!!.attributes)
                lp.title = "modifica il canale"
                lp.width = (resources.displayMetrics.widthPixels * 0.85).toInt()
                lp.height = (resources.displayMetrics.heightPixels * 0.75).toInt()
                dialog.show()
                dialog.window!!.attributes = lp

                val nameCha = dialog.findViewById<EditText>(R.id.name)
                val unitCha = dialog.findViewById<EditText>(R.id.measureUnit)
                val idcnrCha = dialog.findViewById<EditText>(R.id.idCnr)
                val minCha = dialog.findViewById<EditText>(R.id.rangeMin)
                val maxCha = dialog.findViewById<EditText>(R.id.rangeMax)

                nameCha.setText(currentChannel.name() ?: "")
                idcnrCha.setText(currentChannel.idCnr() ?: "")
                unitCha.setText(currentChannel.measureUnit() ?: "")
                minCha.setText(currentChannel.rangeMin()?.toString() ?: "")
                maxCha.setText(currentChannel.rangeMax()?.toString() ?: "")

                dialog.aggiornaC.setOnClickListener {
                    mutate(UpdateChannelMutation(
                        args.channelId,
                        ChannelInput.builder()
                            .name(nameCha.text.toNullableString())
                            .idCnr(idcnrCha.text.toNullableString())
                            .measureUnit(unitCha.text.toNullableString())
                            .rangeMin(minCha.text.toNullableString()?.toDoubleOrNull())
                            .rangeMax(maxCha.text.toNullableString()?.toDoubleOrNull())
                            .build()
                    )).onResult {
                        dialog.dismiss()
                        onChannelLoad()
                    }
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun onDateChange(day: Calendar) {
        val from = day
        val to = from.copy()
        to.add(Calendar.DAY_OF_MONTH, 1)


        swipeContainer.isRefreshing = true

        // RE-request channel(?)
        query(ChannelDetailsQuery(args.channelId, from.time, to.time)).onResult {
            currentChannel = it.channel()
            onDataReceived(day, it.channel().readings())
        }
    }

    fun getPrimaryColor(): Int {
        val typedValue = TypedValue()

        val typedArray = context!!.obtainStyledAttributes(typedValue.data, intArrayOf(R.attr.colorPrimary))
        val color = typedArray.getColor(0, Color.RED)

        typedArray.recycle()

        return color
    }

    fun onDataReceived(day: Calendar, data: List<ChannelDetailsQuery.Reading>) {
        currentDate = day
        activity?.title = currentChannel.name() ?: "Canale"

        view!!.findViewById<TextView>(R.id.date_text).text = getString(R.string.current_date).format(
            userFriendlyDateFormatter.format(day.time))

        val datasets = createData(currentChannel, data, day, getPrimaryColor())

        chart.data = LineData(datasets)
        chart.invalidate()// Refresh

        swipeContainer.isRefreshing = false
    }

    fun createData(channel: ChannelDetailsQuery.Channel, data: List<ChannelDetailsQuery.Reading>, start: Calendar, color: Int): LineDataSet {
        val vals = data.map {
            // Compress coordinates (from the 1970 to the beginning of the day
            // it should fit nicely in a float (with more precision, I hope)
            val diff = it.date().time - start.timeInMillis

            Entry(diff.toFloat(), it.valueMin().toFloat())
        }.sortedBy { it.x }

        val dataSet = LineDataSet(vals, channel.name())
        dataSet.color = color
        dataSet.setCircleColor(color)
        return dataSet
    }

    companion object {
        private const val TAG = "QuickGraph"
        private val userFriendlyDateFormatter = SimpleDateFormat.getDateInstance()
    }
}