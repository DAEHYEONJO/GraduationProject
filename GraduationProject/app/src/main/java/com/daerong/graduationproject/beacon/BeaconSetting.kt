package com.daerong.graduationproject.beacon

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.minew.beaconset.BluetoothState
import com.minew.beaconset.MinewBeacon
import com.minew.beaconset.MinewBeaconManager
import com.minew.beaconset.MinewBeaconManagerListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class BeaconSetting(var context: Context, var activity: TestBeaconActivity) {
    val MAX_BEACON_SECOND = 10
    val MAX_DISTANCE = 100.24f

    private lateinit var mMinewBeaconManager : MinewBeaconManager

    var bDistList = ArrayList<ArrayList<Float>>()
    var beaconResult = ArrayList<Float>()


    var bSecond = 0 //비콘이 시작한 뒤에 지나간 초를 저장


    fun start(){
        bSecond=0
        bDistList.clear()
        beaconResult.clear()
        for(i in 1..4){
            bDistList.add(ArrayList())
        }

        mMinewBeaconManager = MinewBeaconManager.getInstance(context)
        checkBluetooth()

        mMinewBeaconManager.setMinewbeaconManagerListener(object : MinewBeaconManagerListener {
            @SuppressLint("ResourceAsColor")
            override fun onRangeBeacons(p0: MutableList<MinewBeacon>?) {
                Log.d("beaconsize",p0?.size.toString())
                if(bSecond > MAX_BEACON_SECOND) {
                    intervalFinish()
                }
                else {
                    if (p0?.size != 0) {
                        p0?.forEach {
                            Log.d("deacondist", "distance : ${it.distance}, minor : ${it.minor}")
                            when (it.minor) {
                                "52291" -> {
                                    bDistList[0].add(it.distance)
                                }
                                "52292" -> {
                                    bDistList[1].add(it.distance)
                                }
                                "52284" -> {
                                    bDistList[3].add(it.distance)
                                }
                                "52293" -> {
                                    bDistList[2].add(it.distance)
                                }
                            }
                        }
                        bSecond++
                    }
                }
            }


            override fun onDisappearBeacons(p0: MutableList<MinewBeacon>?) {

            }

            override fun onUpdateBluetoothState(p0: BluetoothState?) {

            }

            override fun onAppearBeacons(p0: MutableList<MinewBeacon>?) {

            }


        })
        mMinewBeaconManager.startScan()
    }
    private fun checkBluetooth() {
        val bluetoothState = mMinewBeaconManager.checkBluetoothState()
        when (bluetoothState) {
            BluetoothState.BluetoothStateNotSupported -> {
                //Toast.makeText(this, "Not Support BLE", Toast.LENGTH_SHORT).show()
                Log.d("bluetooth", "Not Support BLE")
            }
            BluetoothState.BluetoothStatePowerOff -> {
                //Toast.makeText(this, "전원 꺼짐 BLE", Toast.LENGTH_SHORT).show()
                Log.d("bluetooth", "전원 꺼짐 BLE")
            }
            BluetoothState.BluetoothStatePowerOn -> {
                //Toast.makeText(this, "전원 켜짐 BLE", Toast.LENGTH_SHORT).show()
                Log.d("bluetooth", "전원 켜짐 BLE")
            }
        }
    }
    @SuppressLint("ResourceAsColor")
    private fun intervalFinish(){
        mMinewBeaconManager.stopScan()
        mMinewBeaconManager.stopService()
        mMinewBeaconManager.setMinewbeaconManagerListener(null)
        Log.d("Beacon", "Beacon Finished")

        for (i in 0..3){
            bDistList[i].sort()
            for(j in bDistList[i]){
                Log.d("beaconResult", "${i+1} : $j")
            }
            val s = bDistList[i].size
            if(s > 0){
                beaconResult.add(bDistList[i][s/2])
            }
            else{
                beaconResult.add(MAX_DISTANCE)
            }
        }

        activity.getResult(beaconResult)
    }


}