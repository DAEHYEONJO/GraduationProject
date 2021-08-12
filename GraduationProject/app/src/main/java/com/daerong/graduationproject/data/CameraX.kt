package com.daerong.graduationproject.data

import android.net.Uri
import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class CameraX(
        var uri : Uri,
        var checked : Boolean = false) : Serializable {
}