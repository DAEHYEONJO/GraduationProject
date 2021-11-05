package com.daerong.graduationproject.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.daerong.graduationproject.R
import com.daerong.graduationproject.carlist.CarListFragment
import com.daerong.graduationproject.data.InsertCar
import com.daerong.graduationproject.databinding.ActivitySelectFunctionBinding
import com.daerong.graduationproject.insertcar.InsertCarActivity
import com.daerong.graduationproject.insertcar.InsertCarInfoFragment
import com.daerong.graduationproject.radio.RadioFragment
import com.daerong.graduationproject.service.ExitCarService


class SelectFunctionActivity : AppCompatActivity() {
    private val REQ_RECORD_PERMISSIONS = 212
    lateinit var binding : ActivitySelectFunctionBinding
    val carListFragment = CarListFragment()
    val radioFragment = RadioFragment()
    private val permissions = arrayOf(Manifest.permission.RECORD_AUDIO,Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySelectFunctionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (intent.hasExtra("carInfo")){
            val carInfo = intent?.getSerializableExtra("carInfo") as InsertCar
            Toast.makeText(this,"onCreate ${carInfo.carNum} / ${carInfo.parkingLotName}", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this,"전달된 intent 없음", Toast.LENGTH_SHORT).show()
        }
        initService()
        initBottomNavigation()

        if (!isGranted()){
            ActivityCompat.requestPermissions(this@SelectFunctionActivity,permissions,REQ_RECORD_PERMISSIONS)
        }
    }

    private fun isGranted() : Boolean{
        permissions.forEach {permission->
            if (ActivityCompat.checkSelfPermission(this@SelectFunctionActivity,permission)!= PackageManager.PERMISSION_GRANTED){
                return false
            }
        }
        return true
    }

    private fun initBottomNavigation() {
        binding.bottomNavigation.setOnNavigationItemSelectedListener {
            when(it.itemId){
                R.id.car_list_menu ->{
                    val fragment = supportFragmentManager.beginTransaction()
                    //fragment.addToBackStack(null)
                    fragment.replace(R.id.main_fragment, carListFragment)
                    fragment.commit()
                }
                R.id.insert_car_menu->{
                    val intent = Intent(this, InsertCarActivity::class.java)
                    startActivity(intent)
                }
                /*R.id.radio_menu->{
                    val fragment = supportFragmentManager.beginTransaction()
                    fragment.replace(R.id.main_fragment, radioFragment)
                    fragment.commit()
                }*/
            }
            true
        }
        binding.bottomNavigation.selectedItemId = R.id.car_list_menu

    }

    private fun initService() {
        val intent = Intent(this@SelectFunctionActivity, ExitCarService::class.java)
        startForegroundService(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent!=null){
            if (intent.hasExtra("carInfo")){
                Log.d("SelectFunctionActivity","intent 도착")
                val carInfo = intent?.getSerializableExtra("carInfo") as InsertCar
                Toast.makeText(this,"onNewIntent ${carInfo.carNum} / ${carInfo.parkingLotName}", Toast.LENGTH_SHORT).show()
            } else{
                Log.d("SelectFunctionActivity","intent 없음")
                Toast.makeText(this,"onNewIntent 전달없음", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode){
            REQ_RECORD_PERMISSIONS -> {
                var check = true
                for (granted in grantResults){
                    if (granted != PackageManager.PERMISSION_GRANTED){
                        check = false
                        break
                    }
                }
                if (!check){
                    Toast.makeText(this, "녹음 권한을 확인해야 합니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

}