package com.daerong.graduationproject

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.daerong.graduationproject.databinding.ActivityMainBinding
import com.minew.beaconset.BluetoothState
import com.minew.beaconset.MinewBeacon
import com.minew.beaconset.MinewBeaconManager
import com.minew.beaconset.MinewBeaconManagerListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    lateinit var binding : ActivityMainBinding
    lateinit var beaconDataList : ArrayList<BeaconData>

    private lateinit var mMinewBeaconManager : MinewBeaconManager
    lateinit var beaconAdapter: BeaconAdapter
    val scope = CoroutineScope(Dispatchers.Main)

    var sumOrigin : Double = 0.0
    var sumTrans : Double = 0.0
    val alpha = 0.25
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initRecyclerView()
        binding.beaconBtn.setOnClickListener {
            init()
        }




    }

    private fun batterySetting(){
        val i = Intent()
        val packageName = packageName
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        if(pm.isIgnoringBatteryOptimizations(packageName)){
            i.action = Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
        }else{
            i.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
            i.data = Uri.parse("package:$packageName")
        }
        startActivity(i)
    }

    private fun init(){
        mMinewBeaconManager = MinewBeaconManager.getInstance(this)
        PermissionCheck(this@MainActivity).requestPermission(Manifest.permission.ACCESS_COARSE_LOCATION,1)
        //PermissionCheck(this@MainActivity).requestPermission(Manifest.permission.ACCESS_COARSE_LOCATION,1)
        checkBluetooth()

        mMinewBeaconManager.setMinewbeaconManagerListener(object : MinewBeaconManagerListener{
            override fun onRangeBeacons(p0: MutableList<MinewBeacon>?) {
                Log.d("tq",p0?.size.toString())
                if (p0?.size != 0){
                    p0?.forEach {
                        val ratio = calculateDistance(it.txpower.toInt(),it.rssi.toDouble())
                        Log.d("beacon_value","ratio : ${ratio}")
                        beaconAdapter.list.add(0, BeaconData(it.minor,it.rssi,it.distance,ratio))
                        beaconAdapter.notifyItemInserted(0)
                        scope.launch {
                            binding.beaconList.smoothScrollToPosition(0)
                        }
                        Log.d("beacon_value","device id : ${it.deviceId}")
                        Log.d("beacon_value","rssi : ${it.rssi.toString()}")
                        Log.d("beacon_value","txpower : ${it.txpower.toString()}")
                        Log.d("beacon_value","distance : ${it.distance.toString()}")
                        Log.d("beacon_value","macAddress : ${it.macAddress}")
                        Log.d("beacon_value","minor : ${it.minor}")
                        Log.d("beacon_value","uuid : ${it.uuid}")
                        //Log.d("beacon_value","uuid ${it.uuid}")
                        //Log.d("beacon_value","name ${it.name}")



                    }
                }
            }

            fun calculateDistance(txPower: Int, rssi: Double): Double {
                if (rssi == 0.0) {
                    return -1.0 // if we cannot determine distance, return -1.
                }
                val ratio = rssi * 1.0 / txPower
                return if (ratio < 1.0) {
                    Math.pow(ratio, 10.0)
                } else {
                    0.89976 * Math.pow(ratio, 7.7095) + 0.111
                }
            }

            override fun onDisappearBeacons(p0: MutableList<MinewBeacon>?) {

            }

            override fun onUpdateBluetoothState(p0: BluetoothState?) {

            }

            override fun onAppearBeacons(p0: MutableList<MinewBeacon>?) {

            }

            override fun equals(other: Any?): Boolean {
                return super.equals(other)
            }

            override fun hashCode(): Int {
                return super.hashCode()
            }
        })
        mMinewBeaconManager.startScan()
    }

    private fun checkBluetooth() {
        val bluetoothState = mMinewBeaconManager.checkBluetoothState()
        when (bluetoothState) {
            BluetoothState.BluetoothStateNotSupported -> {
                Toast.makeText(this, "Not Support BLE", Toast.LENGTH_SHORT).show()
            }
            BluetoothState.BluetoothStatePowerOff -> {
                Toast.makeText(this, "전원 꺼짐 BLE", Toast.LENGTH_SHORT).show()
            }
            BluetoothState.BluetoothStatePowerOn -> {
                Toast.makeText(this, "전원 켜짐 BLE", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun initRecyclerView(){
        binding.apply {
            beaconDataList = ArrayList<BeaconData>()
            beaconAdapter = BeaconAdapter(ArrayList<BeaconData>())
            beaconList.layoutManager = LinearLayoutManager(this@MainActivity,LinearLayoutManager.VERTICAL,false)
            beaconList.addItemDecoration(DividerItemDecoration(this@MainActivity,LinearLayoutManager.VERTICAL))
            beaconList.adapter = beaconAdapter
        }

    }
}
