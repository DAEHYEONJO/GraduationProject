package com.daerong.graduationproject

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.daerong.graduationproject.databinding.ActivityMain2Binding
import org.altbeacon.beacon.*
import java.util.*


class MainActivity2 : AppCompatActivity(), BeaconConsumer {
    lateinit var binding: ActivityMain2Binding
    lateinit var beaconManager: BeaconManager

    lateinit var sectionList: MutableList<TextView>


    @SuppressLint("ResourceAsColor")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMain2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        sectionList = arrayOf(binding.a,binding.b,binding.c,binding.d,binding.e,binding.f,binding.g,binding.h,binding.i).toMutableList()

        beaconManager = BeaconManager.getInstanceForApplication(this)
        beaconManager.beaconParsers.add(BeaconParser().setBeaconLayout("m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25"))
        beaconManager.bind(this)

        binding.changeBtn.setOnClickListener {
            for(i in sectionList){
                i.setTextColor(R.color.park_section_alpha_color)
                i.setBackgroundResource(R.drawable.park_line)
            }
            val r = Random()
            val x = r.nextInt(624).toDouble()
            val y = r.nextInt(624).toDouble()
            Log.d("sectionxy","x : $x y : $y")
            Log.d("sectionxy","x/208 : ${x.toInt()/208} y/208 : ${y.toInt()/208}")
            changeResultSection(calcSection(x,y))
        }

    }
    private fun calcSection(x : Double, y : Double) : Int {
        when (x.toInt()/208){
            0->{
                return if(y.toInt()/208 == 0) 0
                else if(y.toInt()/208 == 1) 3
                else 6
            }
            1->{
                return if(y.toInt()/208 == 0) 1
                else if(y.toInt()/208 == 1) 4
                else 7
            }
            2->{
                return if(y.toInt()/208 == 0) 2
                else if(y.toInt()/208 == 1) 5
                else 8
            }
        }
        return -1
    }

    private fun changeResultSection(index : Int){
        sectionList[index].setBackgroundResource(R.drawable.park_line_find)
        sectionList[index].setTextColor(Color.BLACK)
    }
    override fun onBeaconServiceConnect() {
        beaconManager.removeAllMonitorNotifiers()
        beaconManager.addMonitorNotifier(object : MonitorNotifier{
            override fun didDetermineStateForRegion(state: Int, region: Region?) {

            }

            override fun didEnterRegion(region: Region?) {
                Log.d("beaconLibrary",region?.uniqueId.toString() + "enter!")
            }

            override fun didExitRegion(region: Region?) {

            }
        })
        beaconManager.addRangeNotifier { beacons, region ->
            if (beacons.isNotEmpty()){
                Log.d("beaconLibrary","rssi : "+beacons.iterator().next().rssi.toString())
                Log.d("beaconLibrary","runningAverageRssi : "+beacons.iterator().next().runningAverageRssi.toString())
                Log.d("beaconLibrary","bluetoothName : "+beacons.iterator().next().bluetoothName)
                Log.d("beaconLibrary","id1 : "+beacons.iterator().next().id1.toString())
                Log.d("beaconLibrary","id2 : "+beacons.iterator().next().id2.toString())
                Log.d("beaconLibrary","id3 : "+beacons.iterator().next().id3.toString())
                Log.d("beaconLibrary","distance : "+beacons.iterator().next().distance.toString())
            }else{
                Log.d("beaconLibrary","empty ")
            }
        }
        beaconManager.startMonitoringBeaconsInRegion(Region("jodaehyeon",Identifier.parse("E2C56DB5-DFFB-48D2-B060-D0F5A71096E0"),null,null))
        beaconManager.startRangingBeaconsInRegion(Region("jodaehyeon",Identifier.parse("E2C56DB5-DFFB-48D2-B060-D0F5A71096E0"),null,null))
    }

    override fun onDestroy() {
        super.onDestroy()
        beaconManager.unbind(this)
    }
}