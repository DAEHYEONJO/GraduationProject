package com.daerong.graduationproject.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.daerong.graduationproject.R
import com.daerong.graduationproject.data.InsertCar
import com.daerong.graduationproject.insertcar.InsertCarActivity
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.*

class ExitCarService : Service() {

    private var notificationId = 10
    private lateinit var manager: NotificationManager

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        Log.d("ExitCarService", "onCreate")
        super.onCreate()
        makeNotification()
    }

    private fun makeNotification() {
        val channel = NotificationChannel("channel1","eohyeryungbabo"
            ,NotificationManager.IMPORTANCE_NONE)
        manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
        val builder = NotificationCompat.Builder(this,"channel1")
            .setSmallIcon(R.drawable.ic_baseline_gps_fixed_24)
            .setContentTitle("백그라운드용")
            .setContentText("안보였으면")
        val notification = builder.build()
        startForeground(notificationId,notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("ExitCarService", "onStartCommand")
        observeCarState()
        return START_STICKY//시스템에 의해 종료되면 재시작하기
    }

    private fun observeCarState(){
        val db = Firebase.firestore.collection("CarList")
        db.addSnapshotListener { value, error ->
            if (error!=null) return@addSnapshotListener

            for (doc in value!!.documentChanges){
                if (doc.type == DocumentChange.Type.MODIFIED){
                    val curDocument = doc.document.data
                    val carStatus = curDocument["carStatus"].toString().toInt()
                    if (carStatus == 1){
                        val carNum = curDocument["carNum"].toString()
                        val parkingLotName = curDocument["parkingLotName"].toString()
                        val parkingSection = curDocument["parkingSection"].toString()
                        Log.d("ExitCarService", "${carNum}//${parkingLotName}//${parkingSection}")
                        makePendingIntent(carNum,parkingLotName,parkingSection)
                    }
                }
            }
        }
    }

    private fun makePendingIntent(c_num:String, c_name:String, c_section : String){
        val id = "exitCar${c_num}"
        val name = "whoAccept"
        val notificationChannel = NotificationChannel(id,name,NotificationManager.IMPORTANCE_HIGH)
        notificationChannel.apply {
            enableLights(true)
            enableVibration(true)
            lightColor = Color.BLUE
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        val notificationBuilder = NotificationCompat.Builder(this,id)
        notificationBuilder.setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("출차 알람 메세지")
            .setContentText("$c_num / $c_name / $c_section")
            .setAutoCancel(true)
            .setDefaults(Notification.FLAG_NO_CLEAR)

        val intent = Intent(this, InsertCarActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK

        val pendingIntent = PendingIntent.getActivities(this,100, arrayOf(intent),PendingIntent.FLAG_UPDATE_CURRENT)
        notificationBuilder.setContentIntent(pendingIntent)

        val notification = notificationBuilder.build()

        manager.createNotificationChannel(notificationChannel)
        manager.notify(++notificationId,notification)

    }
}