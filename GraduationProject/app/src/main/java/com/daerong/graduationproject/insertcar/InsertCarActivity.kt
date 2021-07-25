package com.daerong.graduationproject.insertcar

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.daerong.graduationproject.R
import com.daerong.graduationproject.databinding.ActivityInsertCarBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng

class InsertCarActivity : AppCompatActivity() {
    lateinit var binding : ActivityInsertCarBinding
    lateinit var mapFr : SupportMapFragment
    lateinit var googleMap : GoogleMap
    val seoulStation = LatLng(37.55548946601854, 126.97087284635012)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInsertCarBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initMap()
    }

    private fun initMap() {
        mapFr = supportFragmentManager.findFragmentById(R.id.maps) as SupportMapFragment
        mapFr.getMapAsync {
            googleMap = it
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(seoulStation,16f))
        }
    }
}