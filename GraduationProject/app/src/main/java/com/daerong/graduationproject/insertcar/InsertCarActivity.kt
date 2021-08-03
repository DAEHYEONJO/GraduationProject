package com.daerong.graduationproject.insertcar

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import com.daerong.graduationproject.R
import com.daerong.graduationproject.adapter.CustomInfoWindowAdapter
import com.daerong.graduationproject.application.GlobalApplication
import com.daerong.graduationproject.data.DistName
import com.daerong.graduationproject.data.InsertCar
import com.daerong.graduationproject.data.ParkingLot
import com.daerong.graduationproject.databinding.ActivityInsertCarBinding
import com.daerong.graduationproject.databinding.CustomMapInfoWindowBinding
import com.daerong.graduationproject.viewmodel.InsertCarViewModel
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.lang.Math.cos
import java.lang.Math.pow
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.asin
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.properties.Delegates

class InsertCarActivity : AppCompatActivity() {

    private lateinit var binding : ActivityInsertCarBinding
    private lateinit var mapFr : SupportMapFragment
    private lateinit var googleMap : GoogleMap
    private val kuHospital = LatLng(37.54101163580981, 127.07222089798883)
    private val kuKulHouse = LatLng(37.539310989047806, 127.07798674029614)
    private val hangJeongKwan = LatLng(37.54340621359558, 127.0742615457416)
    private val saeCheonNyeon = LatLng(37.54393231655204, 127.07708146729868)
    private val locArray = arrayOf(saeCheonNyeon,kuHospital,kuKulHouse,hangJeongKwan)//얘들어간부분 최종적으로 다 지우기
    private val kuUniv = LatLng(37.541943028352364, 127.07511985263763)

    private val permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    private val REQ_PERMISSION = 100
    private val REQ_RE = 200
    //fine location : use gps, coarse location :

    private lateinit var fusedLocationClient : FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private lateinit var markMap: HashMap<LatLng,Marker>

    private val insertCarViewModel : InsertCarViewModel by viewModels<InsertCarViewModel>()
    private val db = GlobalApplication.db
    private val parkingLotMap = HashMap<LatLng,ParkingLot>()//db에서 가져올때 viewmodel 초기화 용도 변수
    private val mutex = Mutex()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInsertCarBinding.inflate(layoutInflater)
        fetchParkingLotData()
        initValuesForDbTest()
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

    private fun getDistance(lng1: LatLng, lng2 : LatLng) : Double{
        val dLat = Math.toRadians(lng1.latitude-lng2.latitude)//위도 차이
        val dLong = Math.toRadians(lng1.longitude-lng2.longitude)//경도차이

        val lat1 = Math.toRadians(lng1.latitude)//경도1 라디안으로
        val lat2 = Math.toRadians(lng2.latitude)//경도 2 라디안으로

        val a = sin(dLat / 2).pow(2.0) + sin(dLong / 2).pow(2)* kotlin.math.cos(lat1) * kotlin.math.cos(lat2)
        val radius = 6371//지구반지름
        val c = 2* asin(sqrt(a))//두점사이 각도
        return radius*c*1000
    }

    private fun calcClosestPlace(loc: LatLng): String {
        val priorityQueue = PriorityQueue<DistName>()
        insertCarViewModel.parkingLotMap.value!!.keys.forEach { latLng ->
            val dist1 = getDistance(loc,latLng)
            priorityQueue.add(DistName(dist1,insertCarViewModel.parkingLotMap.value!![latLng]!!.parkingLotName))
        }
        return priorityQueue.poll().name
    }

    private fun initValuesForDbTest() {
        GlobalApplication.prefs.setString("id","jmkqpt@hanmail.net")//
        insertCarViewModel.run {
            curCarNum.value = "111가2222"
            curParkingLotSection.value = "A"

        }
    }


    private fun initBtn() {
        binding.apply {
            myLocation.setOnClickListener {
                setUpdateLocationListener()
            }
            completeBtn.setOnClickListener {
                insertCurCar()
            }
        }
    }

    private fun insertCurCar() {
        //1. 필수 정보(주차장이름, 차량번호, 주차구역정)의 not null 여부 확인하기
        val curParkingLotName = insertCarViewModel.curParkingLotName.value
        val curParkingSection = insertCarViewModel.curParkingLotSection.value
        val curCarNum = insertCarViewModel.curCarNum.value
        if (curParkingLotName!=""&&curParkingSection!=""&&curCarNum!=""){
            Log.e("insertCurCar","not null insertCurCar")
            //필수정보 not null
            //2. 현재 선택된 주차장의 curCarCount와 maxCarCount 비교하여 입차 가능 여부 확인
            val docRef = db.collection("ParkingLot").document(curParkingLotName!!)
            var curCarCount = -1
            var maxCarCount = -1
            docRef.get().addOnSuccessListener {
                Log.e("insertCurCar","addOnSuccessListener insertCurCar")
                curCarCount = it["curCarCount"].toString().toInt()
                maxCarCount = it["maxCarCount"].toString().toInt()
                if (curCarCount<maxCarCount){
                    //curCarCount +1 하기
                    //차량 객체 생성해서 db에 등록하기
                    docRef.update("curCarCount",curCarCount+1)
                    val insertCar = InsertCar(
                        parkingLotName = curParkingLotName,
                        parkingSection = curParkingSection!!,
                        carNum = curCarNum!!,
                        carStatus = 2,
                        managed = GlobalApplication.prefs.getString("id","")
                    )
                    db.collection("CarList").document(curCarNum).set(insertCar)
                }
            }
        }else{
            //필수정보 Null
        }

    }

    private fun fetchParkingLotData(){
        markMap = HashMap()
        db.collection("ParkingLot").addSnapshotListener { value, error ->
            if (error!=null) return@addSnapshotListener

            for (doc in value!!.documentChanges){
                Log.e("fetchParkingLotData", "addmarker for loop")
                val data = doc.document.data

                when (doc.type) {
                    DocumentChange.Type.ADDED -> Log.d("fetchParkingLotData", "New city: ${doc.document.data}")
                    DocumentChange.Type.MODIFIED -> Log.d("fetchParkingLotData", "Modified city: ${doc.document.data}")
                    DocumentChange.Type.REMOVED -> Log.d("fetchParkingLotData", "Removed city: ${doc.document.data}")
                }
                val location = doc.document.data["location"] as GeoPoint
                Log.d("fetchParkingLotData", location.latitude.toString())
                val parkingLot = ParkingLot(
                        parkingLotName = data["parkingLotName"].toString(),
                        location = LatLng(location.latitude,location.longitude),
                        curCarCount = data["curCarCount"].toString().toInt(),
                        maxCarCount = data["maxCarCount"].toString().toInt()
                )
                parkingLotMap.put(parkingLot.location,parkingLot)
                Log.d("fetchParkingLotData", parkingLotMap.get(parkingLot.location).toString())
                if (doc.type == DocumentChange.Type.ADDED){
                    CoroutineScope(Dispatchers.Main).launch {
                        mutex.withLock {
                            Log.e("fetchParkingLotData", "addmarker loop")
                            addMarker(parkingLot.location)
                        }
                    }
                }

            }
            insertCarViewModel.parkingLotMap.value=parkingLotMap
            insertCarViewModel.parkingLotMap.value!!.entries.forEach {
                Log.d("fetchParkingLotData", "insertCarViewModel parkingLotMap ${it.value}")
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun setUpdateLocationListener(){
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val locationRequest = LocationRequest.create()
        locationRequest.run {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 1000
        }
        locationCallback = object : LocationCallback(){
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult?.let {
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
        Log.d("dist : ",calcClosestPlace(myLocation).toString())
        /*googleMap.clear()
        addAllMarkers()
        addMarker(myLocation)
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLocation,16f))*/
    }

    private fun addAllMarkers(){
        Log.d("fetchParkingLotData", "addAllMarkers")
        markMap = HashMap();
        for (latLng in locArray) {
            val options = MarkerOptions()
            options.position(latLng)
            options.icon(BitmapDescriptorFactory.fromResource(R.drawable.parking_black))
            val marker = googleMap.addMarker(options)
            markMap.put(latLng,marker)
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
            CoroutineScope(Dispatchers.Main).launch {
                mutex.withLock {

                    googleMap=it
                    Log.e("fetchParkingLotData", "googleMap init")
                    markerClickListener()
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(kuUniv,16f))
                }
            }

            //addAllMarkers()
        }
    }

    private fun addMarker(loc : LatLng){
        Log.e("fetchParkingLotData", "addmarker")
        val options = MarkerOptions()
        options.position(loc)
        options.icon(BitmapDescriptorFactory.fromResource(R.drawable.parking_black))
        val marker = googleMap.addMarker(options)
        markMap.put(loc,marker)
    }

    private fun markerClickListener(){
        googleMap.setOnMarkerClickListener { p0 ->
            insertCarViewModel.parkingLotMap.value?.keys?.forEach {
                Log.d("markerClickListener", "마커클릭 검은색세팅 ${insertCarViewModel.parkingLotMap.value!![it]?.parkingLotName}")
                markMap[it]?.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.parking_black))
            }
            Log.d("markerClickListener", insertCarViewModel.parkingLotMap.value?.get(p0.position).toString())
            p0?.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.parking_here))

            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(p0.position,16f))

            insertCarViewModel.curParkingLotName.value = insertCarViewModel.parkingLotMap.value?.get(p0.position)?.parkingLotName.toString()
            Log.d("markerClickListener", insertCarViewModel.curParkingLotName.value.toString() )

            googleMap.setInfoWindowAdapter(CustomInfoWindowAdapter(this@InsertCarActivity, CustomMapInfoWindowBinding.inflate(layoutInflater), insertCarViewModel, p0.position))
            p0.showInfoWindow()
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

    override fun onResume() {
        super.onResume()

    }
}