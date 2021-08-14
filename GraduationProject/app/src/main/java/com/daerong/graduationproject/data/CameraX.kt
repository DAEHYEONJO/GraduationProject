package com.daerong.graduationproject.data

import android.net.Uri
import com.google.gson.annotations.SerializedName
import java.io.File
import java.io.Serializable

data class CameraX(
        var file : File,
        var checked : Boolean = false) : Serializable {
}