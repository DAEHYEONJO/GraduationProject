package com.daerong.graduationproject

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.daerong.graduationproject.databinding.ActivityTestDrawBinding
import com.daerong.graduationproject.draw.BeaconRangeView

class TestDrawActivity : AppCompatActivity() {
    lateinit var binding : ActivityTestDrawBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTestDrawBinding.inflate(layoutInflater)
        setContentView(binding.root)


    }
}