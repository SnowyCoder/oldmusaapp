package com.cnr_isac.oldmusa

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat.startActivity

object Constants {
    const val GITHUB_LINK = "https://github.com/OldMusa-5H/"

    fun goToGithubPage(context: Context, suffix: String = "") {
        val uriUrl = Uri.parse(GITHUB_LINK + suffix)
        val launchBrowser = Intent(Intent.ACTION_VIEW, uriUrl)
        startActivity(context, launchBrowser, null)
    }
}