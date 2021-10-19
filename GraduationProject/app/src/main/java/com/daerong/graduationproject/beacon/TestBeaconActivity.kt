package com.daerong.graduationproject.beacon

import android.Manifest
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.daerong.graduationproject.PermissionCheck
import com.daerong.graduationproject.databinding.ActivityTestBeaconBinding
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter

class TestBeaconActivity : AppCompatActivity() {
    lateinit var binding: ActivityTestBeaconBinding
    var content = ""
    lateinit var beaconsetting: BeaconSetting
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTestBeaconBinding.inflate(layoutInflater)
        setContentView(binding.root)
        beaconsetting = BeaconSetting(applicationContext, this)
        PermissionCheck(this).requestPermission(Manifest.permission.ACCESS_COARSE_LOCATION,1)
        initBtn()
    }
    fun initBtn(){
        binding.btnA.setOnClickListener {
            content = "select A > "
            beaconsetting.start()
        }
        binding.btnB.setOnClickListener {
            content = "select B > "
            beaconsetting.start()
        }
        binding.btnC.setOnClickListener {
            content = "select C > "
            beaconsetting.start()
        }
        binding.btnD.setOnClickListener {
            content = "select D > "
            beaconsetting.start()
        }
    }
    fun getResult(result: ArrayList<Float>){
        val idx = result.indexOf(result.minOrNull())
        binding.textView.text = idx.toString()
        for(i in result){
            content += "$i "
        }
        val filePath = filesDir.path + "/beacondata.txt"
        Log.d("Test", "path : $filePath")
        writeTextToFile(filePath)
    }
    fun writeTextToFile(path: String) {
        val file = File(path)
        val fileWriter = FileWriter(file, true)
        val bufferedWriter = BufferedWriter(fileWriter)

        bufferedWriter.append(content)
        bufferedWriter.newLine()
        bufferedWriter.close()
        Log.d("beaconResult : ", content)
    }
}