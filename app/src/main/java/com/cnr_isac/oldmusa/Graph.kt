package com.cnr_isac.oldmusa

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import kotlinx.android.synthetic.main.activity_sensor.*
import java.util.ArrayList
import com.github.mikephil.charting.data.LineData
import android.R.attr.x
import android.graphics.Color


class Graph : AppCompatActivity() {

    lateinit var LineChart: LineChart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_graph)

        val lineChart = findViewById<LineChart>(R.id.lineChart)

        val xAXES = ArrayList<String>()
        val yAXESsin = ArrayList<Entry>()
        val yAXEScos = ArrayList<Entry>()

        var x : Double = 0.0
        val numDataPoints : Int = 1000
        val i : Int


        for (i in 0 until numDataPoints) {
            val sinFunction = java.lang.Float.parseFloat(Math.sin(x).toString())
            val cosFunction = java.lang.Float.parseFloat(Math.cos(x).toString())
            x = (x + 0.1).toInt().toDouble()
            yAXESsin.add(Entry(sinFunction, i.toFloat()))
            yAXEScos.add(Entry(cosFunction, i.toFloat()))
            xAXES.add(i, x.toString())
        }
        val xaxes = arrayOfNulls<String?>(xAXES.size)
        for (i in 0 until xAXES.size) {
            xaxes[i] = xAXES.get(i).toString()
        }

        val lineDataSets = ArrayList<ILineDataSet>()
        val lineDataSet1 = LineDataSet(yAXEScos, "cos")
        lineDataSet1.setDrawCircles(false)
        lineDataSet1.color = Color.BLUE

        val lineDataSet2 = LineDataSet(yAXESsin, "sin")
        lineDataSet2.setDrawCircles(false)
        lineDataSet2.color = Color.RED

        lineDataSets.add(lineDataSet1)
        lineDataSets.add(lineDataSet2)

        lineChart.data = LineData(xaxes, lineDataSets)
        lineChart.setVisibleXRangeMaximum(65f)
    }
    }

}