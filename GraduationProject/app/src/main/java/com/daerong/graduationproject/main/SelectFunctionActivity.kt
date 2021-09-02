package com.daerong.graduationproject.main

import android.Manifest
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import com.daerong.graduationproject.R
import com.daerong.graduationproject.carlist.CarListFragment
import com.daerong.graduationproject.data.InsertCar
import com.daerong.graduationproject.databinding.ActivitySelectFunctionBinding
import com.daerong.graduationproject.insertcar.InsertCarActivity
import com.daerong.graduationproject.insertcar.InsertCarInfoFragment
import com.daerong.graduationproject.service.ExitCarService


class SelectFunctionActivity : AppCompatActivity() {
    lateinit var binding : ActivitySelectFunctionBinding
    val carListFragment = CarListFragment()
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
                R.id.radio_menu->{

                }
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

}