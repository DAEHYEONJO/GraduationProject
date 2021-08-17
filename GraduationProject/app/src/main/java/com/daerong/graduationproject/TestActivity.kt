package com.daerong.graduationproject

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.bumptech.glide.Glide
import com.daerong.graduationproject.databinding.ActivityTestBinding

class TestActivity : AppCompatActivity() {
    lateinit var binding : ActivityTestBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Glide.with(this).asGif().load(R.drawable.loading).into(binding.imgView)
    }
}