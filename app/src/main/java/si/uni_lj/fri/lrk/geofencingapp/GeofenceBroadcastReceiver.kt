package si.uni_lj.fri.lrk.geofencingapp

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.text.TextUtils
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent

class GeofenceBroadcastReceiver: BroadcastReceiver() {

    companion object {
        const val TAG = "GeofenceBroadcastReceiver"
        const val CHANNELID = "si.uni_lj.fri.lrk.geofencingapp.GEOFENCING_EVENTS"
        const val NOTIFICATIONID = 101
    }

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(TAG, "onReceive")

        if (context != null && intent != null) {

            createChannel(CHANNELID, "Geofencing", "Reporting geofencing events", context)

            processGeofenceEvent(context, intent)

        }

    }

    private fun processGeofenceEvent(context: Context, intent: Intent) {

        Log.d(TAG, "processGeofenceEvent")

        val geofencingEvent = GeofencingEvent.fromIntent(intent)

        if (geofencingEvent == null) Log.d(TAG, "processGeofenceEvent is null")

        geofencingEvent?.let {
            if(it.hasError()) {
                val errorMessage = GeofenceStatusCodes
                    .getStatusCodeString(geofencingEvent.errorCode)
                Log.d(TAG, errorMessage)
                return
            }
        }

        val geofenceTransition = geofencingEvent?.geofenceTransition

        Log.d(TAG, "geofenceTransition $geofenceTransition")

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
            geofenceTransition == Geofence.GEOFENCE_TRANSITION_DWELL ||
            geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT){

            Log.d(TAG, "geofenceTransition + $geofenceTransition")

            val triggeringGeofences = geofencingEvent.triggeringGeofences

            triggeringGeofences?.let {
                val geofenceTransitionTitle = getGeofenceTitle(geofenceTransition, triggeringGeofences, context)

                val geofenceTransitionDetails = getToDoString(triggeringGeofences, context)

                sendNotification(geofenceTransitionTitle, geofenceTransitionDetails, context)
            }
        }
    }

    private fun getGeofenceTitle(geofenceTransition: Int, triggeringGeofences: List<Geofence>, context: Context): String {

        val geofenceTransitionString = when(geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> context.getString(R.string.geofence_transition_entered)
            Geofence.GEOFENCE_TRANSITION_DWELL -> context.getString(R.string.geofence_transition_dwelled)
            Geofence.GEOFENCE_TRANSITION_EXIT -> context.getString(R.string.geofence_transition_exited)
            else -> context.getString(R.string.unknown_geofence_transition)
        }

        val triggeringGeofencesIdsList = ArrayList<String>()
        for (geofence in triggeringGeofences) {
            triggeringGeofencesIdsList.add(geofence.requestId)
        }

        val triggeringGeofencesIdsString = TextUtils.join(", ", triggeringGeofencesIdsList)

        return "$geofenceTransitionString: $triggeringGeofencesIdsString"
    }

    private fun getToDoString(triggeringGeofences: List<Geofence>, context: Context): String {

        var todoString = ""
        for (geofence in triggeringGeofences) {
            if (geofence.requestId.equals(context.getString(R.string.map_marker_home))) {
                todoString += context.getString(R.string.todo_home)
            }
            if (geofence.requestId.equals(context.getString(R.string.map_marker_work))) {
                todoString += context.getString(R.string.todo_work)
            }
            if (geofence.requestId.equals(context.getString(R.string.map_marker_fitness))) {
                todoString += context.getString(R.string.todo_fitness)
            }
        }
        return todoString
    }


    private fun createChannel(id: String, name: String, desc: String, context : Context) {

        Log.d(TAG, "createChannel")

        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(id, name, importance)
        with(channel) {
            description = desc
            lightColor = Color.RED
            enableLights(true)
        }

        with(NotificationManagerCompat.from(context)) {
            createNotificationChannel(channel)
        }
    }


    @SuppressLint("MissingPermission")
    private fun sendNotification(notificationTitle: String, notificationDetails: String, context: Context) {
        Log.i(TAG, "Trying to send")
        //  Send notification
        val notificationIntent = Intent(context, MapsActivity::class.java)
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val notifPendingIntent = PendingIntent.getActivity(
            context, 0, notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notif = NotificationCompat.Builder(context, CHANNELID)
            .setSmallIcon(android.R.drawable.star_on)
            .setContentTitle(notificationTitle)
            .setContentText(notificationDetails)
            .setContentIntent(notifPendingIntent)
            .setAutoCancel(true)
            .build()

        with(NotificationManagerCompat.from(context)) {
            notify(NOTIFICATIONID, notif)
        }
    }



}