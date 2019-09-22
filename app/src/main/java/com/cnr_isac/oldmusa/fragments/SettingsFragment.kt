package com.cnr_isac.oldmusa.fragments


import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.cnr_isac.oldmusa.Account
import com.cnr_isac.oldmusa.Constants
import com.cnr_isac.oldmusa.R

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val apiUrl = findPreference<Preference>("serverUri")!!

        apiUrl.setOnPreferenceClickListener {
            val builder = AlertDialog.Builder(context!!)
            builder.setTitle("Server URI")

            // Set up the input
            val input = EditText(context!!)
            // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
            input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            input.setText(Account.getUrl(context!!))
            builder.setView(input)

            // Set up the buttons
            builder.setPositiveButton("Save") { dialog, which ->
                Account.setUrl(context!!, input.text.toString())
            }

            // TODO try
            // builder.setNeutralButton("Try") { dialog, which ->}

            builder.show()
            true
        }

        val feedback = findPreference<Preference>("feedback")!!
        feedback.setOnPreferenceClickListener {
            Constants.goToGithubPage(context!!, "/issues/new")
            true
        }

    }
}
