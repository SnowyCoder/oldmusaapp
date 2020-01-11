package it.cnr.oldmusa.charter

import android.content.Context
import android.util.AttributeSet
import com.github.mikephil.charting.charts.LineChart

class CustomLineChart @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LineChart(context, attrs, defStyleAttr) {

    override fun init() {
        mViewPortHandler = CustomViewPortHandler()
        super.init()
    }
}