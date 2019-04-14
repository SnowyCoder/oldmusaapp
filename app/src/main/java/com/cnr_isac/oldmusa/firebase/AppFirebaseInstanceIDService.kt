package com.cnr_isac.oldmusa.firebase

import android.util.Log
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.iid.FirebaseInstanceIdService

class AppFirebaseInstanceIDService : FirebaseInstanceIdService() {
    override fun onTokenRefresh() {
        super.onTokenRefresh()

        val newToken = FirebaseInstanceId.getInstance().token
        Log.d("TOKEN", newToken)
    }
}