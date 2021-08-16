package com.daerong.graduationproject.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.bumptech.glide.Glide
import com.daerong.graduationproject.R
import com.daerong.graduationproject.data.InsertCar
import com.daerong.graduationproject.insertcar.InsertCarActivity
import com.daerong.graduationproject.main.SelectFunctionActivity
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.*

class ExitCarService : Service() {

    private val REQUEST_CODE = 100
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
        Log.d("ExitCarService", "observeCarState")
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
                        val managed = curDocument["managed"].toString()
                        val car = InsertCar(parkingLotName = parkingLotName,parkingSection = parkingSection, carNum = carNum, carStatus = 2, managed = managed)
                        Log.d("ExitCarService", "${carNum}//${parkingLotName}//${parkingSection}")
                        makePendingIntent(car)
                    }
                }
            }
        }
    }

    private fun makePendingIntent(car : InsertCar){
        val id = "exitCar${car.carNum}"
        val name = "whoAccept"
        val sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()
        val notificationChannel = NotificationChannel(id,name,NotificationManager.IMPORTANCE_HIGH)
        notificationChannel.apply {
            enableLights(true)
            enableVibration(true)
            lightColor = Color.BLUE
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setSound(sound,audioAttributes)
        }
        val notificationLayout = RemoteViews(packageName,R.layout.notification_exit_car_small)
        val notificationLayoutExpanded = RemoteViews(packageName,R.layout.notification_exit_car_large)
        notificationLayout.setTextViewText(R.id.car_num_small,car.carNum)
        notificationLayout.setTextViewText(R.id.parking_lot_name_small,car.parkingLotName)
        notificationLayout.setTextViewText(R.id.parking_lot_section_small,car.parkingSection)
        notificationLayout.setImageViewResource(R.id.img_small,R.drawable.ic_baseline_alarm_24)

        notificationLayoutExpanded.setTextViewText(R.id.car_num_large,car.carNum)
        notificationLayoutExpanded.setTextViewText(R.id.parking_lot_name_large,car.parkingLotName)
        notificationLayoutExpanded.setTextViewText(R.id.parking_lot_section_large,car.parkingSection)
        notificationLayoutExpanded.setImageViewResource(R.id.img_large,R.drawable.ic_baseline_alarm_24)

        val notificationBuilder = NotificationCompat.Builder(this,id)
        notificationBuilder.setSmallIcon(R.drawable.ic_launcher_foreground)
                .setStyle(NotificationCompat.DecoratedCustomViewStyle())
                .setCustomContentView(notificationLayout)
                .setCustomBigContentView(notificationLayoutExpanded)
                .setAutoCancel(true)
                .setDefaults(Notification.FLAG_NO_CLEAR)

        val intent = Intent(this, SelectFunctionActivity::class.java)
        intent.putExtra("carInfo",car)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK

        val pendingIntent = PendingIntent.getActivities(this,REQUEST_CODE, arrayOf(intent),PendingIntent.FLAG_UPDATE_CURRENT)
        notificationBuilder.setContentIntent(pendingIntent)

        val notification = notificationBuilder.build()

        manager.createNotificationChannel(notificationChannel)
        manager.notify(++notificationId,notification)

    }
}