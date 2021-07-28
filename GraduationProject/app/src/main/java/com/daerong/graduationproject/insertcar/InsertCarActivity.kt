package com.daerong.graduationproject.insertcar

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.daerong.graduationproject.R
import com.daerong.graduationproject.databinding.ActivityInsertCarBinding
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*

class InsertCarActivity : AppCompatActivity() {

    private lateinit var binding : ActivityInsertCarBinding
    private lateinit var mapFr : SupportMapFragment
    private lateinit var googleMap : GoogleMap
    private val kuHospital = LatLng(37.54101163580981, 127.07222089798883)
    private val kuKulHouse = LatLng(37.539310989047806, 127.07798674029614)
    private val hangJeongKwan = LatLng(37.54340621359558, 127.0742615457416)
    private val saeCheonNyeon = LatLng(37.54393231655204, 127.07708146729868)
    private val locArray = arrayOf(kuHospital,kuKulHouse,hangJeongKwan,saeCheonNyeon)
    private val kuUniv = LatLng(37.541943028352364, 127.07511985263763)

    private val permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    private val REQ_PERMISSION = 100
    private val REQ_RE = 200
    //fine location : use gps, coarse location :

    private lateinit var fusedLocationClient : FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private lateinit var markArray: HashMap<LatLng,Marker>

    private val insertCarViewModel : InsertCarViewModel by viewModels<InsertCarViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInsertCarBinding.inflate(layoutInflater)

        val actionBar = supportActionBar
        actionBar?.hide()

        setContentView(binding.root)
        if (isGranted()){
            initMap()
            initBtn()
        }else{
            ActivityCompat.requestPermissions(this@InsertCarActivity,permissions,REQ_PERMISSION)
        }
    }

    private fun initBtn() {
        binding.apply {
            myLocation.setOnClickListener {
                setUpdateLocationListener()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun setUpdateLocationListener(){
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val locationRequest = LocationRequest.create()
        locationRequest.run {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 2000
        }
        locationCallback = object : LocationCallback(){
            var count = 0
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult?.let {
                    count++
                    for((i,location ) in it.locations.withIndex()){
                        Log.d("fuesedLocationCallback","$i ${location.latitude}, ${location.longitude}")
                        setLastLocation(location)
                    }
                }
            }
        }
        //로케이션 요청 함수 호출(locationRequest, locationCallback)

        if (!isGranted()){
            ActivityCompat.requestPermissions(this@InsertCarActivity,permissions,REQ_PERMISSION)
        }else{
            if (!checkLocationServiceStatus()){
                showLocationServicesSetting()
            }else{
                fusedLocationClient.requestLocationUpdates(locationRequest,locationCallback, Looper.myLooper())
            }
        }
    }

    private fun setLastLocation(location: Location) {
        val myLocation = LatLng(location.latitude,location.longitude)
        googleMap.clear()
        addAllMarkers()
        addMarker(myLocation)
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLocation,16f))
    }

    private fun addAllMarkers(){
        markArray = HashMap();
        for (latLng in locArray) {
            val options = MarkerOptions()
            options.position(latLng)
            options.icon(BitmapDescriptorFactory.fromResource(R.drawable.parking_black))
            val marker = googleMap.addMarker(options)
            markArray.put(latLng,marker)
        }
    }

    private fun checkLocationServiceStatus(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))//로케이션매니저의 gps가 이용 가능한지 상태정보 확인하여 반환받기
    }

    private fun isGranted() : Boolean{
        permissions.forEach {permission->
            if (ActivityCompat.checkSelfPermission(this@InsertCarActivity,permission)!=PackageManager.PERMISSION_GRANTED){
                return false
            }
        }
        return true
    }

    private fun initMap() {
        mapFr = supportFragmentManager.findFragmentById(R.id.maps) as SupportMapFragment
        mapFr.getMapAsync { it ->
            googleMap = it
            markerClickListener()
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(kuUniv,16f))
            addAllMarkers()
        }
    }

    private fun addMarker(loc : LatLng){
        val options = MarkerOptions()
        options.position(loc)
        options.icon(BitmapDescriptorFactory.fromResource(R.drawable.parking_black))
        googleMap.addMarker(options)
    }

    private fun markerClickListener(){
        googleMap.setOnMarkerClickListener { p0 ->
            locArray.forEach {
                markArray[it]?.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.parking_black))
            }
            p0?.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.parking_here))

            true
        }
    }

    private fun showLocationServicesSetting(){
        val builder = AlertDialog.Builder(this@InsertCarActivity)
        builder.setTitle("권한 설정")
            .setMessage("요청한 권한을 수락해주세요.")
            .setPositiveButton("권한 설정") { dialog, which ->
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivityForResult(intent,REQ_RE)
            }
            .setNegativeButton("취소") { dialog, which ->
                Toast.makeText(this@InsertCarActivity, "일부 기능이 제한됩니다.", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .create()
            .show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            REQ_PERMISSION->{
                var check = true
                for (granted in grantResults){
                    if (granted != PackageManager.PERMISSION_GRANTED){
                        check = false
                        break
                    }
                }
                if (check){
                    initMap()
                }else{
                    Toast.makeText(this, "위치 권한을 확인해야 합니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode){
            REQ_RE->{
                if (isGranted()){
                    initMap()
                }
            }
        }
    }
}