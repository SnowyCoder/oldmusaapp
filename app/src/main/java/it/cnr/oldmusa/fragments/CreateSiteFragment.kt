package it.cnr.oldmusa.fragments

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import it.cnr.oldmusa.AddSiteMutation
import it.cnr.oldmusa.CnrSiteIdsQuery
import it.cnr.oldmusa.R
import it.cnr.oldmusa.UpdateSiteMutation
import it.cnr.oldmusa.type.SiteInput
import it.cnr.oldmusa.util.AndroidUtil.alwaysComplete
import it.cnr.oldmusa.util.AndroidUtil.toNullableString
import it.cnr.oldmusa.util.AndroidUtil.useLoadingBar
import it.cnr.oldmusa.util.GraphQlUtil.mutate
import it.cnr.oldmusa.util.GraphQlUtil.query
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.fragment_create_site.*
import kotlinx.android.synthetic.main.fragment_create_site.view.*

class CreateSiteFragment : Fragment() {

    val args: CreateSiteFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        val view = inflater.inflate(R.layout.fragment_create_site, container, false)

        val details = args.details
        if (details == null) {
            activity?.title = "Aggiungi Sito"
            view.add.text = "Aggiungi"
        } else {
            activity?.title = "Modifica Sito"
            view.add.text = "Aggiorna"

            view.name.setText(details.name ?: "")
            view.idCnr.setText(details.idCnr ?: "")
        }

        view.idCnr.alwaysComplete()

        query(CnrSiteIdsQuery()).onResult {
            view.idCnr.setAdapter(ArrayAdapter(context!!, android.R.layout.simple_list_item_1, it.cnrSiteIds()))
        }

        view.add.setOnClickListener {
            onConfirm()
        }

        return view
    }

    fun onConfirm() {
        val name = this.name.text?.toNullableString()
        val idCnr = this.idCnr.text?.toNullableString()

        val details = args.details
        val op = if (details == null) { // Create
            AddSiteMutation.builder()
                .name(name)
                .idCnr(idCnr)
                .build()
        } else { // Update
            UpdateSiteMutation(
                details.id, SiteInput.builder()
                    .name(name)
                    .idCnr(idCnr)
                    .build()
            )
        }
        mutate(op).onResult {
            findNavController().popBackStack()
        }.useLoadingBar(this)
    }

    @Parcelize
    data class SiteDetails (
        val id: Int,
        val name: String?,
        val idCnr: String?
    ) : Parcelable

    companion object {
        const val TAG = "CreateSite"
    }
}