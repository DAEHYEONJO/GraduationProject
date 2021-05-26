package com.daerong.graduationproject

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat

class PermissionCheck(private val activity: Activity) {

    private fun isGranted(permission : String) : Boolean {
        if(ActivityCompat.checkSelfPermission(activity, permission)!=PackageManager.PERMISSION_GRANTED) {
            return false
        }
        return true
    }

    fun requestPermission(permission: String, requsetCode : Int){
        if (!isGranted(permission)){
            Log.d("tq","$permission 은 이미 수락되었음")
            ActivityCompat.requestPermissions(activity, arrayOf(permission),requsetCode)
        }else{
            Toast.makeText(activity, "$permission 권한 수락 완료", Toast.LENGTH_SHORT).show()
        }
    }
}