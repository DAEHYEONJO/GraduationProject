package com.daerong.graduationproject.data

import android.net.Uri
import java.io.Serializable

data class InsertCar(var parkingLotName: String = "주차장 미선택"
                     , var parkingSection: String = "미선택"
                     , var carNum: String = "차량번호 미인식"
                     , var carPhotoUri: Uri = Uri.EMPTY
                     , var carStatus: Int = 3
                     , var approachStatus: Boolean = false
                     , var managed: String = ""):Serializable
//차량상태
// 0 : 출차대기중, 1 : 출차중, 2 : 입차, 3 : 아무것도아닌상태
{
}