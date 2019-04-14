package com.cnr_isac.oldmusa.firebase


import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.support.v4.app.NotificationCompat
import android.util.Log
import com.cnr_isac.oldmusa.Account.api
import com.cnr_isac.oldmusa.Account.getApi
import com.cnr_isac.oldmusa.Login
import com.cnr_isac.oldmusa.R
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

class AppFirebaseMessagingService : FirebaseMessagingService() {
    /**
     * Incrementing notification id used for the alarms
     */
    var notificationId = 0

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    // [START receive_message]
    override fun onMessageReceived(remoteMessage: RemoteMessage?) {
        // [START_EXCLUDE]
        // There are two types of messages data messages and notification messages. Data messages are handled
        // here in onMessageReceived whether the app is in the foreground or background. Data messages are the type
        // traditionally used with GCM. Notification messages are only received here in onMessageReceived when the app
        // is in the foreground. When the app is in the background an automatically generated notification is displayed.
        // When the user taps on the notification they are returned to the app. Messages containing both notification
        // and data payloads are treated as notification messages. The Firebase console always sends notification
        // messages. For more see: https://firebase.google.com/docs/cloud-messaging/concept-options
        // [END_EXCLUDE]

        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        Log.i(TAG, "FCM Message from ${remoteMessage?.from}")

        // Check if message contains a data payload.
        remoteMessage?.data?.let { data ->
            Log.d(TAG, "Message data payload: $data")

            when (data["type"]) {
                "sensor_range_alarm" -> {
                    val siteName = data["site_name"]
                    val sensorName = data["sensor_name"]
                    val channelName = data["channel_name"]
                    val value = data["value"]
                    Log.i(TAG, "Received notification!")
                    sendAlarm(
                        "Sensor alarm",
                        "$siteName $sensorName $channelName reported $value",
                        NotificationManager.IMPORTANCE_MAX,
                        Intent(this, Login::class.java)// TODO: link to sensor graph
                    )
                }
                else -> {
                    Log.e(TAG, "Unknown FCM message received: ${data["type"]}")
                }
            }
        }

        // Check if message contains a notification payload.
        remoteMessage?.notification?.let {
            Log.i(TAG, "Message Notification Body: ${it.body}")
            sendAlarm(
                it.title!!, it.body!!, NotificationManager.IMPORTANCE_HIGH,
                Intent(this, Login::class.java)
            )
        }

        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendAlarm method below.
    }
    // [END receive_message]

    // [START on_new_token]
    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the InstanceID token
     * is initially generated so this is where you would retrieve the token.
     */
    override fun onNewToken(token: String?) {
        Log.d(TAG, "Refreshed token: $token")

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // Instance ID token to your app server.
        sendRegistrationToServer(token)
    }
    // [END on_new_token]

    /**
     * Persist token to third-party servers.
     *
     * Modify this method to associate the user's FCM InstanceID token with any server-side account
     * maintained by your application.
     *
     * @param token The new token.
     */
    private fun sendRegistrationToServer(token: String?) {
        if (api.getCurrentToken().isNullOrBlank()) return
        FirebaseUtil.publishFCMToken(api)
    }


    private fun sendAlarm(title: String, messageBody: String, importance: Int, intent: Intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
            PendingIntent.FLAG_ONE_SHOT)

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this)
            .setSmallIcon(R.drawable.alert)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(false)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setChannelId("alarm")

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("alarm", getString(R.string.channel_name), importance)
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(notificationId++ /* ID of notification */, notificationBuilder.build())
    }

    companion object {
        private const val TAG = "MyFirebaseMsgService"
    }
}