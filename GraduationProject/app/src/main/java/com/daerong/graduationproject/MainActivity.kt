package com.daerong.graduationproject

import android.Manifest
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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

    private lateinit var mMinewBeaconManager : MinewBeaconManager
    lateinit var beaconAdapter: BeaconAdapter
    val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.beaconBtn.setOnClickListener {
            init()
        }

        //initRecyclerView()


    }

    private fun init(){
        mMinewBeaconManager = MinewBeaconManager.getInstance(this)
        PermissionCheck(this@MainActivity).requestPermission(Manifest.permission.ACCESS_COARSE_LOCATION,1)
        //PermissionCheck(this@MainActivity).requestPermission(Manifest.permission.ACCESS_COARSE_LOCATION,1)
        checkBluetooth()
        beaconAdapter = BeaconAdapter(ArrayList<MinewBeacon>())
        mMinewBeaconManager.setMinewbeaconManagerListener(object : MinewBeaconManagerListener{
            override fun onRangeBeacons(p0: MutableList<MinewBeacon>?) {
                Log.d("tq",p0?.size.toString())
                if (p0?.size != 0){
                    p0?.forEach {
                        Log.d("beacon_value","device id : ${it.deviceId}")
                        Log.d("beacon_value","rssi : ${it.rssi.toString()}")
                        Log.d("beacon_value","txpower : ${it.txpower.toString()}")
                        Log.d("beacon_value","distance : ${it.distance.toString()}")
                    }
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
            beaconList.layoutManager = LinearLayoutManager(this@MainActivity,LinearLayoutManager.VERTICAL,false)
            beaconList.adapter = beaconAdapter
        }

    }
}
