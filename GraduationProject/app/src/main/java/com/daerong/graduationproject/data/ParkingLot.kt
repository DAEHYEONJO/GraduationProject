package com.daerong.graduationproject.data

import com.google.android.gms.maps.model.LatLng
import java.io.Serializable

data class ParkingLot(var parkingLotName: String="",
                      var location: LatLng=LatLng(0.0,0.0),
                      var curCarCount: Int=-1,
                      var maxCarCount: Int=-1) : Serializable {
}