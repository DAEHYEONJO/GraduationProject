package com.daerong.graduationproject.viewmodel

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.daerong.graduationproject.data.ParkingLot
import com.google.android.gms.maps.model.LatLng
import java.util.*

class InsertCarViewModel:ViewModel() {

    var parkingLotMap = MutableLiveData<HashMap<LatLng, ParkingLot>>()

    var curParkingLotName = MutableLiveData<String>()
    var curCarNum = MutableLiveData<String>()
    var curParkingLotSection = MutableLiveData<String>()
    var carPhotoUri = MutableLiveData<Uri>()

    init {
        curParkingLotName.value = ""
        curCarNum.value = ""
        curParkingLotSection.value = ""
    }

}