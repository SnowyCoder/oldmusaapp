package it.cnr.oldmusa.fragments


import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.navigation.fragment.navArgs
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.exception.ApolloNetworkException
import it.cnr.oldmusa.Account
import it.cnr.oldmusa.Constants
import it.cnr.oldmusa.R
import it.cnr.oldmusa.VersionQuery
import it.cnr.oldmusa.util.AndroidUtil.useLoadingBar
import it.cnr.oldmusa.util.GraphQlUtil
import it.cnr.oldmusa.util.GraphQlUtil.apolloCall
import it.cnr.oldmusa.util.SemVer
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

class SettingsFragment : PreferenceFragmentCompat() {

    val args: SettingsFragmentArgs by navArgs()

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
            builder.setPositiveButton("Save") { _, _ ->
                Account.setUrl(context!!, input.text.toString())
            }

            builder.setNeutralButton("Try") { dialog, which -> }

            val dialog = builder.show()

            val posButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE)

            input.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable) {
                    if ((input.text.toString() + "graphql").toHttpUrlOrNull() == null) {
                        input.error = "Invalid url"
                        posButton.isEnabled = false
                    } else {
                        input.error = null
                        posButton.isEnabled = true
                    }
                }

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                }
            })

            dialog.getButton(DialogInterface.BUTTON_NEUTRAL).setOnClickListener {
                val okHttp = Account.getHttpClient(requireContext())


                val url = (input.text.toString() + "graphql").toHttpUrlOrNull()

                if (url == null) {
                    input.error = "Invalid url"
                    return@setOnClickListener
                }

                val tryPollo = ApolloClient.builder()
                    .serverUrl(url)
                    .okHttpClient(okHttp)
                    .build()


                apolloCall(tryPollo.query(VersionQuery()), manageError = false)
                    .onResult {
                        val serverSemVer = SemVer.parseOrNull(it.apiVersion())
                        if (serverSemVer == null) {
                            input.error = "Invalid server version ${it.apiVersion()}"
                            return@onResult
                        }

                        if (!GraphQlUtil.CURRENT_VERSION.isForwardsCompatibleTo(serverSemVer)) {
                            input.error = "Incompatible server version: ${it.apiVersion()}"
                        } else {
                            input.error = null
                            Toast.makeText(requireContext(), "Test: Ok", Toast.LENGTH_LONG).show()
                            // set accept button green
                        }
                    }.onError {
                        if (it is ApolloNetworkException) {
                            input.error = "Cannot reach server"
                        } else {
                            input.error = "Error reaching server: $it"
                        }
                    }.useLoadingBar(this, true)
            }

            dialog.show()

            true
        }

        val feedback = findPreference<Preference>("feedback")!!
        feedback.setOnPreferenceClickListener {
            Constants.goToGithubPage(context!!, "/issues/new")
            true
        }

        when (args.quickLink) {
            QuickEditNavs.NONE -> {}
            QuickEditNavs.EDIT_API_URL -> apiUrl.performClick()
        }

    }

    /**
     * Jump straight to something.
     * An example use case is if the server connection does not work, you can make the user
     * jump right to editing the api url.
     */
    enum class QuickEditNavs {
        NONE, EDIT_API_URL,
    }
}
