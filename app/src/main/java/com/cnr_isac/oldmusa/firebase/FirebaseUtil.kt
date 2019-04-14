package com.cnr_isac.oldmusa.firebase

import android.util.Log
import com.cnr_isac.oldmusa.Login
import com.cnr_isac.oldmusa.api.Api
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.iid.FirebaseInstanceId

object FirebaseUtil {
    fun publishFCMToken(api: Api) {
        FirebaseInstanceId.getInstance().instanceId
            .addOnCompleteListener(OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.e(Login.TAG, "getInstanceId failed", task.exception)
                    return@OnCompleteListener
                }

                // Get new Instance ID token
                val token = task.result!!.token

                // Log and push
                Log.d(Login.TAG, "Publishing FCM id: $token")

                api.getMe().addContactFCM(token)
            })
    }
}