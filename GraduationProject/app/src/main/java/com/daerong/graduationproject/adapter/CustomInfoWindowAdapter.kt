package com.daerong.graduationproject.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.observe
import com.daerong.graduationproject.data.ParkingLot
import com.daerong.graduationproject.databinding.CustomMapInfoWindowBinding
import com.daerong.graduationproject.viewmodel.InsertCarViewModel
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker

class CustomInfoWindowAdapter(var context : Context,var binding: CustomMapInfoWindowBinding, var insertCarViewModel: InsertCarViewModel, var loc : LatLng) : GoogleMap.InfoWindowAdapter {

    private fun setInfoWindowText(){
        insertCarViewModel.parkingLotMap.observe(context as LifecycleOwner, Observer {
            val curParkingLot = it[loc]
            Log.d("CustomInfoWindowAdapter","${curParkingLot!!.curCarCount}")
            binding.title.text = curParkingLot.parkingLotName + " 주차장"
            binding.curCarCountView.text = "현재 입차 차량 수 : ${curParkingLot.curCarCount}"
            binding.maxCarCountView.text = "입차 가능 차량 수 : ${curParkingLot.maxCarCount}"

        })
        Log.d("CustomInfoWindowAdapter","setInfoWindowText 마지막")
    }

   override fun getInfoWindow(p0: Marker?): View {
        Log.d("CustomInfoWindowAdapter","getInfoWindow")
        setInfoWindowText()
        return binding.root
    }

    override fun getInfoContents(p0: Marker?): View {
        Log.d("CustomInfoWindowAdapter","getInfoContents")
        setInfoWindowText()
        return binding.root
    }
}