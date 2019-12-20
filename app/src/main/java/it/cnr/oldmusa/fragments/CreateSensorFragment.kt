package it.cnr.oldmusa.fragments


import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import it.cnr.oldmusa.AddSensorMutation
import it.cnr.oldmusa.CnrSensorIdsQuery
import it.cnr.oldmusa.R
import it.cnr.oldmusa.UpdateSensorMutation
import it.cnr.oldmusa.type.SensorCreateInput
import it.cnr.oldmusa.type.SensorUpdateInput
import it.cnr.oldmusa.util.AndroidUtil.alwaysComplete
import it.cnr.oldmusa.util.AndroidUtil.toNullableString
import it.cnr.oldmusa.util.AndroidUtil.useLoadingBar
import it.cnr.oldmusa.util.GraphQlUtil.mutate
import it.cnr.oldmusa.util.GraphQlUtil.query
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.fragment_create_sensor.*
import kotlinx.android.synthetic.main.fragment_create_sensor.view.*

class CreateSensorFragment : Fragment() {

    val args: CreateSensorFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        val view = inflater.inflate(R.layout.fragment_create_sensor, container, false)

        val details = args.details
        if (details == null) {
            activity?.title = "Aggiungi Sensore"
            view.add.text = "Aggiungi"

            view.autoCreate.visibility = View.VISIBLE
        } else {
            activity?.title = "Modifica Sensore"
            view.add.text = "Aggiorna"

            view.autoCreate.visibility = View.GONE

            view.name.setText(details.name ?: "")
            view.idCnr.setText(details.idCnr ?: "")
            view.enabled.isChecked = details.enabled
        }

        view.idCnr.alwaysComplete()

        query(CnrSensorIdsQuery(args.siteId)).onResult {
            view.idCnr.setAdapter(ArrayAdapter(context!!, android.R.layout.simple_list_item_1, it.site().cnrSensorIds()))
        }

        view.add.setOnClickListener {
            onConfirm()
        }

        return view
    }

    private fun onConfirm() {
        val name = this.name.text?.toNullableString()
        val idCnr = this.idCnr.text.toNullableString()
        val enabled = this.enabled.isChecked

        val details = args.details

        val op = if (details == null) { // Create
            AddSensorMutation(
                args.siteId,
                SensorCreateInput.builder()
                    .name(name)
                    .idCnr(idCnr)
                    .enabled(enabled)
                    .autoCreate(this.autoCreate.isChecked)
                    .build()
            )
        } else { // Update
            UpdateSensorMutation(
                details.id,
                SensorUpdateInput.builder()
                    .name(name)
                    .idCnr(idCnr)
                    .enabled(enabled)
                    .build()
            )
        }

        mutate(op).onResult {
            findNavController().popBackStack()
        }.useLoadingBar(this)
    }

    @Parcelize
    data class SensorDetails (
        val id: Int,
        val name: String?,
        val idCnr: String?,
        val enabled: Boolean
    ) : Parcelable
}
