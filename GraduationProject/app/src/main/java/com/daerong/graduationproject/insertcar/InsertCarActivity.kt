package com.daerong.graduationproject.insertcar

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import com.daerong.graduationproject.R
import com.daerong.graduationproject.adapter.CustomInfoWindowAdapter
import com.daerong.graduationproject.application.GlobalApplication
import com.daerong.graduationproject.data.CameraX
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
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.lang.Exception
import java.lang.Math.cos
import java.lang.Math.pow
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.asin
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.properties.Delegates

class InsertCarActivity : AppCompatActivity() {

    private val REQUEST_TAKE_PHOTO: Int = 10
    private val REQUEST_CAMERAX : Int = 300
    private lateinit var binding : ActivityInsertCarBinding
    private lateinit var mapFr : SupportMapFragment
    private lateinit var googleMap : GoogleMap
    private val kuHospital = LatLng(37.54101163580981, 127.07222089798883)
    private val kuKulHouse = LatLng(37.539310989047806, 127.07798674029614)
    private val hangJeongKwan = LatLng(37.54340621359558, 127.0742615457416)
    private val saeCheonNyeon = LatLng(37.54393231655204, 127.07708146729868)
    private val locArray = arrayOf(saeCheonNyeon,kuHospital,kuKulHouse,hangJeongKwan)//얘들어간부분 최종적으로 다 지우기
    private val kuUniv = LatLng(37.541943028352364, 127.07511985263763)

    private val permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    private val REQ_PERMISSION = 100
    private val REQ_RE = 200
    //fine location : use gps, coarse location :

    private lateinit var fusedLocationClient : FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private lateinit var markMap: HashMap<LatLng,Marker>

    private val insertCarViewModel : InsertCarViewModel by viewModels<InsertCarViewModel>()
    private val db = Firebase.firestore

    private val parkingLotMap = HashMap<LatLng,ParkingLot>()//db에서 가져올때 viewmodel 초기화 용도 변수
    private val mutex = Mutex()

    private lateinit var mCurrentPhotoPath : String
    private var curInfoWindowPlace : LatLng? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInsertCarBinding.inflate(layoutInflater)
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

    private fun viewModelFunction() {
        insertCarViewModel.parkingLotMap.observe(this@InsertCarActivity,{changedMap->
            val curMarker = markMap[curInfoWindowPlace]
            if (curMarker?.isInfoWindowShown == true){
                Log.d("curinfowindowplace","infowindow 보여지는중")
                setCurParkLotNameInfoWindow(curInfoWindowPlace!!,curMarker)
            }else{
                Log.d("curinfowindowplace","infowindow 안보여지는중")
            }
        })
    }

    @SuppressLint("MissingPermission")
    private fun initMap() {
        mapFr = supportFragmentManager.findFragmentById(R.id.maps) as SupportMapFragment
        mapFr.getMapAsync { it ->
            val job = CoroutineScope(Dispatchers.Main).launch {
                mutex.withLock {
                    googleMap=it
                    Log.e("fetchParkingLotData", "googleMap init")
                    markerClickListener()
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(kuUniv,16f))
                }
            }
            Log.e("fetchParkingLotData", "여기오나?")

            //addAllMarkers()
            markMap = HashMap()
            viewModelFunction()
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
                            job.join()
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
    }

    private fun fetchParkingLotData(){

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

    private fun initValuesForDbTest() {
        GlobalApplication.prefs.setString("id","jmkqpt@hanmail.net")//
        insertCarViewModel.run {
            curCarNum.value = "333가2222"
            curParkingLotSection.value = "A"

        }
    }

    private fun initBtn() {
        binding.apply {
            myLocation.setOnClickListener {
                //로딩 애니메이션 표시
                GlobalApplication.getInstance().progressOn(this@InsertCarActivity)
                it.isEnabled = false
                setUpdateLocationListener()
            }
            completeBtn.setOnClickListener {
                insertCurCar()

            }
            parkingCamera.setOnClickListener {
                Log.i("cameraclick","cameraclick")
                //captureCamera()
                startActivityForResult(Intent(this@InsertCarActivity,CameraXActivity::class.java),REQUEST_CAMERAX)
            }
            testBtn.setOnClickListener {
                val mediaDir = externalMediaDirs.firstOrNull().let {it->
                    File(it, resources.getString(R.string.app_name)).apply {
                    }
                }
                mediaDir.listFiles().forEach {
                    Log.d("filesList",it.name.toString())
                    it.delete()
                }
                Log.d("filesList",mediaDir.listFiles().size.toString())
            }
        }
    }

    private fun savePhotos() {
        val curCarNum = insertCarViewModel.curCarNum.value
        /*val uploadFiles = ArrayList<File>()
        insertCarViewModel.carPhotoFile.value?.forEach { originFile ->
            val oldFile = File(originFile.toString())
            val addString = "${insertCarViewModel.curCarNum.value}_${oldFile.name}"
            val newNameFile = File(oldFile.parent,addString)
            Log.d("savePhotos", "newNameFile : $newNameFile")
            if (oldFile.exists()){
                Log.d("savePhotos", "뷰모델에 저장된 파일 객체 존재")
                if (oldFile.renameTo(newNameFile)){
                    Log.d("savePhotos", "파일명 변경 성공3")
                    uploadFiles.add(newNameFile)
                }else{
                    Log.d("savePhotos", "파일명 변경 실패")
                }
            }else {
                Log.d("savePhotos", "뷰모델에 저장된 파일 객체 미존재")
            }
        }*/
        val storage = FirebaseStorage.getInstance()
        val imagesRef = storage.reference
        val imageChild = imagesRef.child(curCarNum!!)
        insertCarViewModel.carPhotoFile.value?.forEach {
            imageChild.child(it.name).putFile(Uri.fromFile(it))
                    .addOnSuccessListener {
                        Log.i("savePhotos","success : ${it.toString()}")
                    }.addOnFailureListener {
                        Log.e("savePhotos","fail : ${it.toString()}")
                    }
        }

    }


    private fun createImageFile() : File? {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "JPEG_$timeStamp.jpg"
        var imageFile : File? = null
        val storageDir = File(Environment.getExternalStorageDirectory().toString()+"/Pictures","jodaehyeon")
        if (!storageDir.exists()){
            Log.e("aboutCamera2",storageDir.toString()+"경로미존재 ")
            storageDir.mkdir()
        }
        imageFile = File(storageDir,imageFileName)
        mCurrentPhotoPath = imageFile.absolutePath
        return imageFile
    }

    private fun captureCamera(){
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager)!=null){
            var photoFile : File?=null
            try {
                photoFile = createImageFile()
            }catch (e : IOException){
                Log.e("aboutCamera2",e.toString())
                return
            }
            if (photoFile!=null){
                val providerUri = FileProvider.getUriForFile(this,packageName,photoFile)
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,providerUri)
                startActivityForResult(takePictureIntent,REQUEST_TAKE_PHOTO)
            }
        }else{
            Log.e("aboutCamera2","empty resoleActivity")
        }
    }

    private fun showPictureDialog() {

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
                    savePhotos()
                }else{
                    Toast.makeText(this@InsertCarActivity, "입차 불가", Toast.LENGTH_SHORT).show()
                }
            }
        }else{
            //필수정보 Null
            Toast.makeText(this@InsertCarActivity, "필수 정보 누락", Toast.LENGTH_SHORT).show()
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
                        binding.myLocation.isEnabled = true
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



    private fun calcClosestPlace(loc: LatLng): LatLng {
        val priorityQueue = PriorityQueue<DistName>()
        insertCarViewModel.parkingLotMap.value!!.keys.forEach { latLng ->
            val dist1 = getDistance(loc,latLng)
            priorityQueue.add(DistName(dist1,insertCarViewModel.parkingLotMap.value!![latLng]!!.parkingLotName, latLng))
        }
        return priorityQueue.poll().latLng
    }

    private fun setLastLocation(location: Location) {
        GlobalApplication.getInstance().progressOff()
        val myLocation = LatLng(location.latitude,location.longitude)
        val closestPlace = calcClosestPlace(myLocation)
        Log.d("dist : ", insertCarViewModel.parkingLotMap.value?.get(closestPlace)?.parkingLotName.toString())
        fusedLocationClient.removeLocationUpdates(locationCallback)
        //현재 location 위치 띄워주기(안해도될것같음)
        //거리 추천 주차장 마커 빨갛게

        makeOneMarkerRedAndMove(closestPlace)
        setCurParkLotNameInfoWindow(closestPlace,markMap[closestPlace]!!)
        //animate camera to 주차장으로 옮겨주기
        //
        /*googleMap.clear()
        addAllMarkers()
        addMarker(myLocation)
        */
    }

    private fun setCurParkLotNameInfoWindow(latLng: LatLng, marker: Marker){
        insertCarViewModel.curParkingLotName.value = insertCarViewModel.parkingLotMap.value?.get(latLng)?.parkingLotName.toString()
        googleMap.setInfoWindowAdapter(CustomInfoWindowAdapter(this@InsertCarActivity, CustomMapInfoWindowBinding.inflate(layoutInflater), insertCarViewModel, latLng))
        marker.showInfoWindow()
        curInfoWindowPlace = latLng
    }

    private fun makeOneMarkerRedAndMove(latLng: LatLng){
        markMap.values.forEach {
            it.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.parking_black))
        }
        markMap[latLng]?.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.parking_here))
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng,16f))
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
            makeOneMarkerRedAndMove(p0.position)
            setCurParkLotNameInfoWindow(p0.position,p0)
            true
        }
        googleMap.setOnMapClickListener {
            //infowindow 다 내리기
            //빨간색 마커 검은색으로 바꾸기
            //현재 선택 주차장 미선택으로 바꾸기
            if (curInfoWindowPlace!=null){
                markMap[curInfoWindowPlace]?.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.parking_black))
            }
            //이건 해야할지 모르겠다(주차장이름 없애기)
            //insertCarViewModel.curParkingLotName.value = ""
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
            REQUEST_TAKE_PHOTO -> {
                if (resultCode == RESULT_OK) {
                    try {
                        galleryAddPic()
                    } catch (e: Exception) {
                        Log.e("aboutCamera2", e.toString())
                    }
                } else {
                    Toast.makeText(this, "사진찍기 취소", Toast.LENGTH_SHORT).show()
                }
            }
            REQUEST_CAMERAX->{
                if (resultCode == RESULT_OK){
                    val imgCheckedList = data?.getSerializableExtra("imgCheckedList") as ArrayList<File>
                    insertCarViewModel.carPhotoFile.value = imgCheckedList
                    insertCarViewModel.carPhotoFile.value!!.forEach {
                        Log.d("onActivityResult", "넘어온 viewmodel file : ${it.toString()}")
                    }

                }
            }
        }
    }

    private fun galleryAddPic() {
        Log.i("galleryAddPic","galleryAddPic Call")
        val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
        val f = File(mCurrentPhotoPath)
        val contentUri = Uri.fromFile(f)
        mediaScanIntent.data = contentUri
        sendBroadcast(mediaScanIntent)
    }

    override fun onResume() {
        super.onResume()

    }
}