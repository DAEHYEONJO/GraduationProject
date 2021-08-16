package com.daerong.graduationproject.main

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.daerong.graduationproject.R
import com.daerong.graduationproject.data.InsertCar
import com.daerong.graduationproject.databinding.ActivitySelectFunctionBinding
import com.daerong.graduationproject.service.ExitCarService

class SelectFunctionActivity : AppCompatActivity() {
    lateinit var binding : ActivitySelectFunctionBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySelectFunctionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (intent.hasExtra("carInfo")){
            val carInfo = intent?.getSerializableExtra("carInfo") as InsertCar
            Toast.makeText(this,"onCreate ${carInfo.carNum} / ${carInfo.parkingLotName}", Toast.LENGTH_SHORT).show()
        }else{
            Toast.makeText(this,"전달된 intent 없음", Toast.LENGTH_SHORT).show()
        }
        initService()
    }

    private fun initService() {
        val intent = Intent(this@SelectFunctionActivity, ExitCarService::class.java)
        startForegroundService(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.d("onNewIntent","intent 도착")
        val carInfo = intent?.getSerializableExtra("carInfo") as InsertCar
        Toast.makeText(this,"onNewIntent ${carInfo.carNum} / ${carInfo.parkingLotName}", Toast.LENGTH_SHORT).show()
    }
}