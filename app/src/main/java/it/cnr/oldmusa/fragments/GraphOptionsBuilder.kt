package it.cnr.oldmusa.fragments


import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import it.cnr.oldmusa.R
import it.cnr.oldmusa.util.AndroidUtil
import it.cnr.oldmusa.util.AndroidUtil.dateSelectPopupSetup
import kotlinx.android.synthetic.main.fragment_graph_options_builder.*
import java.util.*

class GraphOptionsBuilder : Fragment() {
    val args: GraphOptionsBuilderArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_graph_options_builder, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val today = Calendar.getInstance()
        endDate.setText(AndroidUtil.isoSimpleDateFormat.format(today.time))
        today.add(Calendar.DAY_OF_MONTH, -1)
        startDate.setText(AndroidUtil.isoSimpleDateFormat.format(today.time))

        startDate.dateSelectPopupSetup()
        endDate.dateSelectPopupSetup()

        this.draw.setOnClickListener {
            val startCal = Calendar.getInstance()
            val endCal = Calendar.getInstance()


            if (!AndroidUtil.parseIsoDate(startCal, this.startDate.text.toString()) ||
                !AndroidUtil.parseIsoDate(endCal, this.endDate.text.toString())) {
                Toast.makeText(context!!, "Invalid date!", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val dirs = GraphOptionsBuilderDirections.actionGraphOptionsBuilderToSensorGraph(
                args.sensorId, startCal.timeInMillis, endCal.timeInMillis)
            findNavController().navigate(dirs)
        }

        super.onViewCreated(view, savedInstanceState)
    }
}
