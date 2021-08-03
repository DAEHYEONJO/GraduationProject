package com.daerong.graduationproject.data

import com.google.android.gms.maps.model.LatLng

data class DistName(var dist : Double, var name : String ) : Comparable<DistName> {
    override fun compareTo(other: DistName): Int {
        return this.dist.toInt() - other.dist.toInt()
    }
}