package com.daerong.graduationproject.main

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.daerong.graduationproject.R
import com.daerong.graduationproject.databinding.ActivitySelectFunctionBinding
import com.daerong.graduationproject.service.ExitCarService

class SelectFunctionActivity : AppCompatActivity() {
    lateinit var binding : ActivitySelectFunctionBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySelectFunctionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initService()
    }

    private fun initService() {
        val intent = Intent(this@SelectFunctionActivity, ExitCarService::class.java)
        startForegroundService(intent)
    }
}