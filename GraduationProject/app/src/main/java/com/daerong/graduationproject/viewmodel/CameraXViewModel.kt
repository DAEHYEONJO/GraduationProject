package com.daerong.graduationproject.viewmodel

import androidx.camera.core.CameraX
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CameraXViewModel : ViewModel(){
    var captureImage = ArrayList<MutableLiveData<CameraX>>()
}