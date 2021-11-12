package com.daerong.graduationproject.carlist

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import com.daerong.graduationproject.R
import com.daerong.graduationproject.data.InsertCar
import com.daerong.graduationproject.databinding.BottomSheetLayoutBinding
import com.daerong.graduationproject.service.GeofenceBroadcastReceiver
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MyBottomSheetDialog(private val carData: InsertCar) : BottomSheetDialogFragment() {
    lateinit var binding: BottomSheetLayoutBinding
    private val db = Firebase.firestore

    private val requestPermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){
        var flag = true
        it.forEach{
            if(it.value == false) flag = false
        }
        if (flag) Toast.makeText(requireContext(), "백그라운드 허용 ", Toast.LENGTH_SHORT).show()
    }

    private val geofenceList : Geofence = getGeofence("신공학관",Pair(37.53991, 127.07075))

    lateinit var geofencingClient : GeofencingClient
    lateinit var geofencePendingIntent : PendingIntent
    private fun addGeofences(carNum : String){
        val pendingIntent = Intent(requireContext(), GeofenceBroadcastReceiver::class.java)
        pendingIntent.putExtra("carNum",carNum)
        pendingIntent.action = "ACTION_GEOFENCE_EVENT"
        geofencePendingIntent = PendingIntent.getBroadcast(requireContext(),2525,pendingIntent,PendingIntent.FLAG_UPDATE_CURRENT)

        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(requireContext(), "퍼미션 등록 안되어이썽", Toast.LENGTH_SHORT).show()
            return
        }
        geofencingClient.addGeofences(getGeofencingRequest(geofenceList),geofencePendingIntent)
            .addOnSuccessListener {
                Log.d("GeofenceBroadcastReceiver","add geofence Success")
                Toast.makeText(requireContext(), "add Success", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Log.e("GeofenceBroadcastReceiver","add geofence fail")
                Toast.makeText(requireContext(), "add Success", Toast.LENGTH_SHORT).show()
            }
    }

    private fun getGeofencingRequest(geofence :Geofence) : GeofencingRequest {
        Log.d("GeofenceBroadcastReceiver","getGeofencingRequest")
        return GeofencingRequest.Builder().apply {
            this.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            this.addGeofence(geofence)
        }.build()
    }

    private fun getGeofence(reqId : String, geo: Pair<Double,Double>, radius : Float = 100f) : Geofence{
        Log.d("GeofenceBroadcastReceiver","getGeofence radius : $radius")
        return Geofence.Builder()
            .setRequestId(reqId)
            .setCircularRegion(geo.first,geo.second,radius)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setLoiteringDelay(1000)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT or Geofence.GEOFENCE_TRANSITION_DWELL)
            .build()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = BottomSheetLayoutBinding.inflate(layoutInflater, container, false)

        return binding.root
    }


    private fun removeGeofence(){
        geofencingClient.removeGeofences(geofencePendingIntent)
                .addOnSuccessListener {
                    Log.d("GeofenceRemove","geofence remove success")
                }.addOnFailureListener {
                    Log.e("GeofenceRemove","geofence remove fail")
                }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requestPermissions.launch(arrayOf(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION))
        geofencingClient = LocationServices.getGeofencingClient(requireContext())
        addGeofences(carData.carNum)
        binding.carNumberView.text = carData.carNum
        binding.destinationView.text = "요거프레소"
        binding.parkingLotView.text = "${carData.parkingLotName} ${carData.parkingSection}구역"
        binding.exitCompleteBtn.setOnClickListener {
            Toast.makeText(context, "출차 완료 버튼 클릭", Toast.LENGTH_SHORT).show()

            removeGeofence()

            val docRef = db.collection("CarList").document(carData.carNum)
            docRef.get().addOnSuccessListener {
                val parkingLot = it["parkingLotName"].toString()
                Log.d("deleteCar", "parkingLotName: $parkingLot")
                val parkRef = db.collection("ParkingLot").document(parkingLot)
                Log.d("deleteCar", "parkRef: $parkRef")
                parkRef.get().addOnSuccessListener { parking ->
                    Log.d("deleteCar", "parking: ${parking["curCarCount"]}")
                    val num = parking["curCarCount"].toString().toInt()
                    parkRef.update("curCarCount", num-1)
                }
                docRef.update("approachStatus", true)
            }

            dismiss()
        }

    }

}