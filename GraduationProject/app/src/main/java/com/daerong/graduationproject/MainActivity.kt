package com.daerong.graduationproject

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.widget.TextView
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
    lateinit var sectionList: MutableList<TextView>

    val MAX_BEACON_SECOND = 30
    val MAX_DISTANCE = 6.24
    val INTERVAL_SECOND = 5
    lateinit var binding : ActivityMainBinding
    lateinit var beaconDataList : ArrayList<BeaconData>

    private lateinit var mMinewBeaconManager : MinewBeaconManager
    lateinit var beaconAdapter: BeaconAdapter
    var bDistanceListA = MutableList(MAX_BEACON_SECOND) { MAX_DISTANCE }
    var bDistanceListB = MutableList(MAX_BEACON_SECOND) { MAX_DISTANCE }
    var bDistanceListC = MutableList(MAX_BEACON_SECOND) { MAX_DISTANCE }
    var bMinListA = MutableList(MAX_BEACON_SECOND/INTERVAL_SECOND) { MAX_DISTANCE }
    var bMinListB = MutableList(MAX_BEACON_SECOND/INTERVAL_SECOND) { MAX_DISTANCE }
    var bMinListC = MutableList(MAX_BEACON_SECOND/INTERVAL_SECOND) { MAX_DISTANCE }

    val bA = BeaconLocation(0.0,0.0)
    val bB = BeaconLocation(0.0,6.24)
    val bC = BeaconLocation(6.24,3.12)
    var distanceA:Double = 0.0
    var distanceB:Double = 0.0
    var distanceC:Double = 0.0

    var bSecond = 0 //비콘이 시작한 뒤에 지나간 초를 저장


    val scope = CoroutineScope(Dispatchers.Main)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sectionList = arrayOf(binding.a,binding.b,binding.c,binding.d,binding.e,binding.f,binding.g,binding.h,binding.i).toMutableList()
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
        bSecond=0
        mMinewBeaconManager = MinewBeaconManager.getInstance(this)
        PermissionCheck(this@MainActivity).requestPermission(Manifest.permission.ACCESS_COARSE_LOCATION,1)
        //PermissionCheck(this@MainActivity).requestPermission(Manifest.permission.ACCESS_COARSE_LOCATION,1)
        checkBluetooth()

        mMinewBeaconManager.setMinewbeaconManagerListener(object : MinewBeaconManagerListener{
            @SuppressLint("ResourceAsColor")
            override fun onRangeBeacons(p0: MutableList<MinewBeacon>?) {
                Log.d("tq",p0?.size.toString())
                if(bSecond>MAX_BEACON_SECOND) {
                    intervalFinish()
                }
                else {
                    if (p0?.size != 0) {
                        p0?.forEach {
                            Log.d("BBBB", "ratio : ${it.txpower}")
                            val ratio = calculateDistance(-51, it.rssi.toDouble())
                            Log.d("BBBB", "ratio : ${ratio}")
                            beaconAdapter.list.add(0, BeaconData(it.minor, it.rssi, it.distance, ratio))
                            beaconAdapter.notifyItemInserted(0)
                            scope.launch {
                                binding.beaconList.smoothScrollToPosition(0)
                            }

                            Log.d("BBBB", "distance : ${it.distance.toString()}")
                            Log.d("BBBB", "minor : ${it.minor}")

                            when (it.minor) {
                                "52291" -> {
                                    distanceA =
                                            if (it.distance.toDouble() > MAX_DISTANCE)
                                                MAX_DISTANCE
                                            else
                                                it.distance.toDouble()

                                    bDistanceListA.add(bSecond, distanceA)
                                    Log.d("BBBB", "minor : ${it.minor}, distance : $distanceA")
                                }
                                "52292" -> {
                                    distanceB =
                                            if (it.distance.toDouble() > MAX_DISTANCE)
                                                MAX_DISTANCE
                                            else
                                                it.distance.toDouble()

                                    bDistanceListB.add(bSecond, distanceB)
                                    Log.d("BBBB", "minor : ${it.minor}, distance : $distanceB")
                                }
                                "52284" -> {
                                    distanceC =
                                            if (it.distance.toDouble() > MAX_DISTANCE)
                                                MAX_DISTANCE
                                            else
                                                it.distance.toDouble()

                                    bDistanceListC.add(bSecond, distanceC)
                                    Log.d("BBBB", "minor : ${it.minor}, distance : $distanceC")
                                }
                            }
                        }
                        bSecond++
                        if (bSecond % INTERVAL_SECOND == 0) {
                            var min = bDistanceListA[bSecond - 5]
                            for (i in 1..4) {
                                var tmp = bDistanceListA[bSecond - i]
                                if (tmp != 0.0 && tmp < min)
                                    min = tmp
                            }
                            bMinListA[bSecond / 5 - 1] = min
                            min = bDistanceListB[bSecond - 5]
                            for (i in 1..4) {
                                var tmp = bDistanceListB[bSecond - i]
                                if (tmp != 0.0 && tmp < min)
                                    min = tmp
                            }
                            bMinListB[bSecond / 5 - 1] = min
                            min = bDistanceListC[bSecond - 5]
                            for (i in 1..4) {
                                var tmp = bDistanceListC[bSecond - i]
                                if (tmp != 0.0 && tmp < min)
                                    min = tmp
                            }
                            bMinListC[bSecond / 5 - 1] = min

                            Log.d("BBBB",
                                    "A : ${bMinListA[bSecond / 5 - 1]}, B : ${bMinListB[bSecond / 5 - 1]}, C : ${bMinListC[bSecond / 5 - 1]}")

                            val new = getLocationWithTrilateration(bA, bB, bC,
                                    bMinListA[bSecond / 5 - 1], bMinListB[bSecond / 5 - 1], bMinListC[bSecond / 5 - 1])
                            Log.d("BBBB", "x : ${new.x}, y : ${new.y}")
                            //여기서 UI갱신
                            for (i in sectionList){
                                i.setTextColor(R.color.park_section_alpha_color)
                                i.setBackgroundResource(R.drawable.park_line)
                            }
                            changeResultSection(calcSection(new.x*100,new.y*100))
                            binding.beaconRangeView.changeRadious(bMinListA[bSecond / 5 - 1].toFloat(), bMinListB[bSecond / 5 - 1].toFloat(), bMinListC[bSecond / 5 - 1].toFloat(),new.x.toFloat(), new.y.toFloat())
                        }
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

    private fun initRecyclerView(){
        binding.apply {
            beaconDataList = ArrayList<BeaconData>()
            beaconAdapter = BeaconAdapter(ArrayList<BeaconData>())
            beaconList.layoutManager = LinearLayoutManager(this@MainActivity,LinearLayoutManager.VERTICAL,false)
            beaconList.addItemDecoration(DividerItemDecoration(this@MainActivity,LinearLayoutManager.VERTICAL))
            beaconList.adapter = beaconAdapter
        }

    }

    @SuppressLint("ResourceAsColor")
    private fun intervalFinish(){
        mMinewBeaconManager.stopScan()
        mMinewBeaconManager.stopService()
        mMinewBeaconManager.setMinewbeaconManagerListener(null)
        Log.d("BBBB", "끝났음")

        bMinListA.sort()
        bMinListB.sort()
        bMinListC.sort()
        bMinListA.sum()
        bMinListB.sum()
        bMinListC.sum()

        for(i in 0 until 6){
            Log.d("BBBB","BeaconA's min : ${bMinListA[i]}")
        }
        Log.d("BBBB","<BeaconA> min : ${bMinListA[0]}, " +
                "average : ${bMinListA.sum()/6}, median : ${(bMinListA[2]+bMinListA[3])/2}")
        for(i in 0 until 6){
            Log.d("BBBB","BeaconB's min : ${bMinListB[i]}")
        }
        Log.d("BBBB","<BeaconB> min : ${bMinListB[0]}, " +
                "average : ${bMinListB.sum()/6}, median : ${(bMinListB[2]+bMinListB[3])/2}")
        for(i in 0 until 6){
            Log.d("BBBB","BeaconC's min : ${bMinListC[i]}")
        }
        Log.d("BBBB","<BeaconC> min : ${bMinListC[0]}, " +
                "average : ${bMinListC.sum()/6}, median : ${(bMinListC[2]+bMinListC[3])/2}")

        var new = getLocationWithTrilateration(bA, bB, bC,
                bMinListA[0], bMinListB[0], bMinListC[0])
        Log.d("BBBB", "<최종 결과 min> x : ${new.x}, y : ${new.y}")

        new = getLocationWithTrilateration(bA, bB, bC,
                bMinListA.sum()/6, bMinListB.sum()/6, bMinListC.sum()/6)
        Log.d("BBBB", "<최종 결과 average> x : ${new.x}, y : ${new.y}")

        new = getLocationWithTrilateration(bA, bB, bC,
                (bMinListA[2]+bMinListA[3])/2, (bMinListB[2]+bMinListB[3])/2, (bMinListC[2]+bMinListC[3])/2)
        Log.d("BBBB", "<최종 결과 median> x : ${new.x}, y : ${new.y}")
        for(i in sectionList){
            i.setTextColor(R.color.park_section_alpha_color)
            i.setBackgroundResource(R.drawable.park_line)
        }
        changeResultSection(calcSection(new.x*100,new.y*100))
        binding.beaconRangeView.changeRadious((bMinListA[2]+bMinListA[3]).toFloat()/2, (bMinListB[2]+bMinListB[3]).toFloat()/2, (bMinListC[2]+bMinListC[3]).toFloat()/2, new.x.toFloat(), new.y.toFloat())
        Toast.makeText(this, "x : ${new.x} y : ${new.y}", Toast.LENGTH_SHORT).show()
    }

    fun getLocationWithTrilateration(
        beaconA: BeaconLocation,
        beaconB: BeaconLocation,
        beaconC: BeaconLocation,
        distanceA: Double,
        distanceB: Double,
        distanceC: Double
    ): BeaconLocation {
        val W: Double
        val Z: Double
        val foundBeaconX: Double
        var foundBeaconY: Double
        val foundBeaconYFilter: Double

        W = distanceA * distanceA - distanceB * distanceB -
                beaconA.x * beaconA.x - beaconA.y * beaconA.y +
                beaconB.x * beaconB.x + beaconB.y * beaconB.y

        Z = distanceB * distanceB - distanceC * distanceC -
                beaconB.x * beaconB.x - beaconB.y * beaconB.y +
                beaconC.x * beaconC.x + beaconC.y * beaconC.y
        foundBeaconX = (W * (beaconC.y - beaconB.y) - Z * (beaconB.y - beaconA.y))/
                (2 * ((beaconB.x - beaconA.x) * (beaconC.y - beaconB.y) - (beaconC.x - beaconB.x) * (beaconB.y - beaconA.y)))
        foundBeaconY = (W - 2 * foundBeaconX * (beaconB.x - beaconA.x)) / (2 * (beaconB.y - beaconA.y))
        //`foundBeaconLongFilter` is a second measure of `foundBeaconLong` to mitigate errors
        foundBeaconYFilter = (Z - 2 * foundBeaconX * (beaconC.x - beaconB.x)) / (2 * (beaconC.y - beaconB.y))
        foundBeaconY = (foundBeaconY + foundBeaconYFilter) / 2
        val newLocation = BeaconLocation(foundBeaconX, foundBeaconY)

        return newLocation
    }
}
