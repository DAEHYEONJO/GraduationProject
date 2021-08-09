package com.daerong.graduationproject.data

import com.google.android.gms.maps.model.LatLng

data class DistName(var dist : Double, var name : String, var latLng: LatLng ) : Comparable<DistName> {
    //name은 없애도됨, 그냥 잘 측정되는지 확인용
    override fun compareTo(other: DistName): Int {
        return this.dist.toInt() - other.dist.toInt()
    }
}