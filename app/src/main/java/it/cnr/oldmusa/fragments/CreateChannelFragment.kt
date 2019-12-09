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
import it.cnr.oldmusa.AddChannelMutation
import it.cnr.oldmusa.CnrChannelIdsQuery
import it.cnr.oldmusa.R
import it.cnr.oldmusa.UpdateChannelMutation
import it.cnr.oldmusa.type.ChannelInput
import it.cnr.oldmusa.util.AndroidUtil.alwaysComplete
import it.cnr.oldmusa.util.AndroidUtil.toNullableString
import it.cnr.oldmusa.util.AndroidUtil.useLoadingBar
import it.cnr.oldmusa.util.GraphQlUtil.mutate
import it.cnr.oldmusa.util.GraphQlUtil.query
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.fragment_create_channel.*
import kotlinx.android.synthetic.main.fragment_create_channel.view.*
import kotlinx.android.synthetic.main.fragment_create_channel.view.idCnr
import kotlinx.android.synthetic.main.fragment_create_channel.view.measureUnit

class CreateChannelFragment : Fragment() {

    val args: CreateChannelFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        val view = inflater.inflate(R.layout.fragment_create_channel, container, false)

        val details = args.details
        if (details == null) {
            activity?.title = "Aggiungi Canale"
            view.add.text = "Aggiungi"
        } else {
            activity?.title = "Modifica Canale"
            view.add.text = "Aggiorna"

            view.name.setText(details.name ?: "")
            view.idCnr.setText(details.idCnr ?: "")
            view.measureUnit.setText(details.measureUnit ?: "")
            view.valueMin.setText(details.rangeMin?.toString() ?: "")
            view.valueMax.setText(details.rangeMax?.toString() ?: "")
        }

        view.idCnr.alwaysComplete()

        query(CnrChannelIdsQuery(args.sensorId)).onResult {
            view.idCnr.setAdapter(ArrayAdapter(context!!, android.R.layout.simple_list_item_1, it.sensor().cnrChannelIds()))
        }

        view.add.setOnClickListener {
            onConfirm()
        }

        return view
    }

    fun onConfirm() {
        val name = this.name.text?.toNullableString()
        val idCnr = this.idCnr.text.toNullableString()
        val measureUnit = this.measureUnit.text?.toNullableString()
        val valueMin = this.valueMin.text?.toNullableString()?.toDoubleOrNull()
        val valueMax = this.valueMax.text?.toNullableString()?.toDoubleOrNull()


        val details = args.details
        val op = if (details == null) { // Create
            AddChannelMutation(
                args.sensorId,
                ChannelInput.builder()
                    .name(name)
                    .idCnr(idCnr)
                    .measureUnit(measureUnit)
                    .rangeMin(valueMin)
                    .rangeMax(valueMax)
                    .build()
            )
        } else { // Update
            UpdateChannelMutation(
                details.id,
                ChannelInput.builder()
                    .name(name)
                    .idCnr(idCnr)
                    .measureUnit(measureUnit)
                    .rangeMin(valueMin)
                    .rangeMax(valueMax)
                    .build()
            )
        }

        mutate(op).onResult {
            findNavController().popBackStack()
        }.useLoadingBar(this)
    }

    @Parcelize
    data class ChannelDetails (
        val id: Int,
        val name: String?,
        val idCnr: String?,
        val measureUnit: String?,
        val rangeMin: Double?,
        val rangeMax: Double?
    ) : Parcelable

}
