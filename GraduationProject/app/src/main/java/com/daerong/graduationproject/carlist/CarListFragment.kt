package com.daerong.graduationproject.carlist

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
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.daerong.graduationproject.TestFragment
import com.daerong.graduationproject.adapter.CarListAdapter
import com.daerong.graduationproject.application.GlobalApplication
import com.daerong.graduationproject.data.InsertCar
import com.daerong.graduationproject.databinding.FragmentCarListBinding
import com.daerong.graduationproject.service.GeofenceBroadcastReceiver
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class CarListFragment : Fragment() {

    //복사변수
    companion object{
        private const val LOCATIONS_REQUEST_CODE: Int = 199
    }
    private val permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    private val geofenceList : MutableList<Geofence> by lazy {
        mutableListOf(getGeofence("신공학관",Pair(37.54093927558642, 127.07933376994532)))
    }
    val geofencingClient : GeofencingClient by lazy {
        LocationServices.getGeofencingClient(requireContext())
    }

    lateinit var geofencePendingIntent : PendingIntent
    //복사변수

    lateinit var binding: FragmentCarListBinding
    lateinit var adapter : CarListAdapter
    private val db = Firebase.firestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCarListBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initPermissions()
        initView()
    }

    fun testFunc(){
        Toast.makeText(requireContext(), "안녕 나야 ", Toast.LENGTH_SHORT).show()
    }

    @SuppressLint("MissingPermission")
    private fun addGeofences(carNum : String){
        val pendingIntent = Intent(requireContext(), GeofenceBroadcastReceiver::class.java)
        pendingIntent.putExtra("carNum",carNum)
        geofencePendingIntent = PendingIntent.getBroadcast(requireContext(),0,pendingIntent,PendingIntent.FLAG_UPDATE_CURRENT)

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
            //addGeofences()
        }else{
            ActivityCompat.requestPermissions(requireActivity(),permissions, LOCATIONS_REQUEST_CODE)
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
                    //addGeofences()
                }else{
                    ActivityCompat.requestPermissions(requireActivity(),permissions, LOCATIONS_REQUEST_CODE)
                }
            }
        }
    }

    private fun initView()  {
        binding.recyclerView.layoutManager = LinearLayoutManager(context,
            LinearLayoutManager.VERTICAL, false)

        val dividerItemDecoration =
            DividerItemDecoration(binding.recyclerView.context, LinearLayoutManager(context).orientation)
        binding.recyclerView.addItemDecoration(dividerItemDecoration)

        var carList = ArrayList<InsertCar>()

        db.collection("CarList").addSnapshotListener { value, error ->
            if (error!=null) return@addSnapshotListener

            for (doc in value!!.documentChanges) {
                Log.d("listsnapshot", "for loop")
                val data = doc.document.data

                when (doc.type) {
                    DocumentChange.Type.ADDED -> {
                        Log.d("listsnapshot", "New Car: ${doc.document.data}")
                        // 입차 상태 or 출차 대기 상태로 들어오는 경우
                        val status = data["carStatus"].toString().toInt()
                        if(status == 2){    // 입차된 경우
                            adapter.items.add(
                                InsertCar(
                                    parkingLotName = data["parkingLotName"].toString(),
                                    parkingSection = data["parkingSection"].toString(),
                                    carNum = data["carNum"].toString(),
                                    carStatus = data["carStatus"].toString().toInt(),
                                    approachStatus = data["approachStatus"].toString().toBoolean(),
                                    managed = data["managed"].toString()
                                )
                            )
                            adapter.notifyItemInserted(adapter.itemCount-1)
                        }
                        else if(status == 1){   // 출차 대기된 경우
                            adapter.items.add(0,
                                InsertCar(
                                    parkingLotName = data["parkingLotName"].toString(),
                                    parkingSection = data["parkingSection"].toString(),
                                    carNum = data["carNum"].toString(),
                                    carStatus = data["carStatus"].toString().toInt(),
                                    approachStatus = data["approachStatus"].toString().toBoolean(),
                                    managed = data["managed"].toString()
                                )
                            )
                            adapter.notifyItemInserted(0)
                        }
                    }
                    DocumentChange.Type.MODIFIED ->{
                        Log.d("listsnapshot", "Modified Car: ${doc.document.data}")
                        // 출차중 or 출차 수락으로 변경되는 경우
                        val car = InsertCar(
                            parkingLotName = data["parkingLotName"].toString(),
                            parkingSection = data["parkingSection"].toString(),
                            carNum = data["carNum"].toString(),
                            carStatus = data["carStatus"].toString().toInt(),
                            approachStatus = data["approachStatus"].toString().toBoolean(),
                            managed = data["managed"].toString()
                        )
                        if (data["carStatus"].toString().toInt() == 1){ // 출차 대기 상태
                            //리스트에서 삭제 후 맨 위로 올리기
                            car.carStatus = 2
                            val index = adapter.items.indexOf(car)
                            if(index!=-1){
                                adapter.items.removeAt(index)
                                adapter.notifyItemRemoved(index)
                            }
                            car.carStatus = 1
                            adapter.items.add(0, car)
                            adapter.notifyItemInserted(0)
                            binding.recyclerView.smoothScrollToPosition(0)
                            Log.d("listsnapshot", "출차 대기로 이동")
                        }
                        else if (data["carStatus"].toString().toInt() == 0){ // 출차 수락 상태
                            car.carStatus = 1
                            val index = adapter.items.indexOf(car)
                            if(index!=-1){
                                adapter.items.removeAt(index)
                                adapter.notifyItemRemoved(index)
                            }
                            Log.d("listsnapshot", "리스트에서 삭제")
                        }

                    }
                    DocumentChange.Type.REMOVED -> {
                        Log.d("listsnapshot", "Removed Car: ${doc.document.data}")

                    }
                }
            }
        }
        adapter = CarListAdapter(carList)
        adapter.btnClickListener = object: CarListAdapter.OnBtnClickListener{
            override fun OnBtnClick(
                holder: RecyclerView.ViewHolder,
                view: View,
                data: InsertCar,
                position: Int
            ) {
                Log.d("listsnapshot", "Exit Btn Click")
                // DB수정, 혹시 미리 바껴 있으면 취소됨
                val docRef = db.collection("CarList").document(data.carNum)
                docRef.get()
                    .addOnSuccessListener {
                        if(it["carStatus"].toString().toInt() == 1){
                            docRef.update("carStatus", 0)
                            val bottomSheet = MyBottomSheetDialog(data)
                            bottomSheet.isCancelable = false
                            bottomSheet.show(childFragmentManager, bottomSheet.tag)

                            //지오펜스추가하기
                            addGeofences(data.carNum)

                        }
                        else{
                            Toast.makeText(context,
                                "이미 출차 중인 차량입니다.", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }

        binding.recyclerView.adapter = adapter
    }


}