package com.daerong.graduationproject

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.daerong.graduationproject.databinding.FragmentTestBinding
import com.daerong.graduationproject.service.GeofenceBroadcastReceiver
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

class TestFragment : Fragment() {

    //복사 변수 목록
    companion object{
        private const val LOCATIONS_REQUEST_CODE: Int = 199
    }
    private val permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    private val geofenceList : MutableList<Geofence> by lazy {
        mutableListOf(getGeofence("신공학관",Pair(37.54093927558642, 127.07933376994532)))
    }
    private val geofencingClient : GeofencingClient by lazy {
        LocationServices.getGeofencingClient(requireContext())
    }
    private val geofencePendingIntent : PendingIntent by lazy {
        val intent = Intent(requireContext(),GeofenceBroadcastReceiver::class.java)
        intent.putExtra("carNum","testcarnum1111")
        PendingIntent.getBroadcast(requireContext(),0,intent,PendingIntent.FLAG_UPDATE_CURRENT)
    }
    //여기까지

    private var binding : FragmentTestBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        binding = FragmentTestBinding.inflate(layoutInflater,container,false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initPermissions()
    }

    @SuppressLint("MissingPermission")
    private fun addGeofences(){
        geofencingClient.addGeofences(getGeofencingRequest(geofenceList),geofencePendingIntent).run {
            addOnSuccessListener {
                Log.d("AddGeofences","add geofence Success")
                Toast.makeText(requireContext(), "add Success", Toast.LENGTH_SHORT).show()
            }
            addOnFailureListener {
                Log.e("AddGeofences","add geofence fail")
                Toast.makeText(requireContext(), "add Success", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getGeofencingRequest(list : List<Geofence>) : GeofencingRequest {
        return GeofencingRequest.Builder().apply {
            this.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            this.addGeofences(list)
        }.build()
    }

    private fun getGeofence(reqId : String, geo: Pair<Double,Double>, radius : Float = 100f) : Geofence{
        return Geofence.Builder()
            .setRequestId(reqId)
            .setCircularRegion(geo.first,geo.second,radius)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
            .build()
    }

    private fun initPermissions() {
        if (isGranted()){
            //geofence 초기화 작업 진행
            Toast.makeText(requireContext(), "퍼미션 등록 완료", Toast.LENGTH_SHORT).show()
            addGeofences()
        }else{
            ActivityCompat.requestPermissions(requireActivity(),permissions,LOCATIONS_REQUEST_CODE)
        }
    }

    private fun isGranted():Boolean{
        permissions.forEach { permission ->
            ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(),permission)
            if (ActivityCompat.checkSelfPermission(requireContext(),permission) != PackageManager.PERMISSION_GRANTED){
                return false
            }
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode){
            LOCATIONS_REQUEST_CODE -> {
                var check = true
                for (granted in grantResults){
                    if (granted != PackageManager.PERMISSION_GRANTED){
                        check = false
                        break
                    }
                }
                if (check){
                    //geofence 초기화 작업
                    addGeofences()
                }else{
                    ActivityCompat.requestPermissions(requireActivity(),permissions,LOCATIONS_REQUEST_CODE)
                }
            }
        }
    }
}