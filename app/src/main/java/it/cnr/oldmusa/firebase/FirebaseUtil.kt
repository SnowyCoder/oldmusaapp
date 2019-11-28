package it.cnr.oldmusa.firebase

import android.util.Log
import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import it.cnr.oldmusa.fragments.LoginFragment
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.iid.FirebaseInstanceId
import it.cnr.oldmusa.AddContactFCMMutation

object FirebaseUtil {
    const val TAG = "Firebase"

    fun publishFCMToken(apolloClient: ApolloClient) {
        FirebaseInstanceId.getInstance().instanceId
            .addOnCompleteListener(OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.e(LoginFragment.TAG, "getInstanceId failed", task.exception)
                    return@OnCompleteListener
                }

                // Get new Instance ID token
                val token = task.result!!.token

                // Log and push
                Log.d(TAG, "Publishing FCM id: $token")

                apolloClient.mutate(AddContactFCMMutation(token)).enqueue(object :
                    ApolloCall.Callback<AddContactFCMMutation.Data>() {
                    override fun onFailure(e: ApolloException) {
                        Log.w(TAG, "Failed to publish FCM", e)
                    }

                    override fun onResponse(response: Response<AddContactFCMMutation.Data>) {
                    }
                })
            })
    }
}