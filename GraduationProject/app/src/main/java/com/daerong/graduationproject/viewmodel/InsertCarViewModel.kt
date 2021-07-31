package com.daerong.graduationproject.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.daerong.graduationproject.data.InsertCar
import com.daerong.graduationproject.data.ParkingLot
import com.google.android.gms.maps.model.LatLng

class InsertCarViewModel:ViewModel() {

    var parkingLotMap = MutableLiveData<HashMap<LatLng,ParkingLot>>()
    var curParkingLotName = MutableLiveData<String>()

}