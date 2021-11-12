package com.daerong.graduationproject.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Toast.makeText(context, "지오펜싱1!!!", Toast.LENGTH_LONG).show()
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent.hasError()) {
            val errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.errorCode)
            Log.d("GeofenceBroadcastReceiver", errorMessage)
            return
        }else{
            Log.d("GeofenceBroadcastReceiver", "지오펜스에러없음")
        }

        val carNum = intent.getStringExtra("carNum")
        Log.d("GeofenceBroadcastReceiver","carNum : $carNum")
        val geofenceTransition = geofencingEvent.geofenceTransition//발생한 이벤트 타입
        //지오펜트 구역에 진입 또는 exit한경우
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER || geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT){

            if (carNum != null) {
                Firebase.firestore.collection("CarList").document(carNum).update("approachStatus",true)
                return
            }

            val triggeringGeofences = geofencingEvent.triggeringGeofences
            val transitionMsg = when(geofenceTransition){
                Geofence.GEOFENCE_TRANSITION_ENTER->"Enter geofence"
                Geofence.GEOFENCE_TRANSITION_EXIT->"Exit geofence"
                else->"else geofence"
            }
            triggeringGeofences.forEach {
                Log.d("GeofenceBroadcastReceiver","id : ${it.requestId} $transitionMsg")
                Toast.makeText(context, "id : ${it.requestId} $transitionMsg", Toast.LENGTH_SHORT).show()
            }
        }else{
            Log.e("GeofenceBroadcastReceiver","onReceived but not event occurred")
        }
    }
}