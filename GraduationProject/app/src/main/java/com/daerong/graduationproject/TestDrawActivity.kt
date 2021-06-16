package com.daerong.graduationproject

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.daerong.graduationproject.databinding.ActivityTestDrawBinding
import com.daerong.graduationproject.draw.BeaconRangeView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

class TestDrawActivity : AppCompatActivity() {
    lateinit var binding : ActivityTestDrawBinding
    val scope = CoroutineScope(Dispatchers.Main)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTestDrawBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.changeBtn.setOnClickListener {
            val random = Random()
            val newRadious1 = 100f + random.nextInt(100)
            val newRadious2 = 100f + random.nextInt(100)
            val newRadious3 = 100f + random.nextInt(100)
            val myx =  random.nextInt(1000).toFloat()
            val myy =random.nextInt(1000).toFloat()
            binding.rangeView.changeRadious(newRadious1,newRadious2,newRadious3,myx,myy)
        }


    }
}