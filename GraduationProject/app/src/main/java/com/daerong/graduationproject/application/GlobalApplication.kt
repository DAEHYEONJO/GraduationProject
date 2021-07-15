package com.daerong.graduationproject.application

import android.app.Application
import com.daerong.graduationproject.R
import com.daerong.graduationproject.login.MyPreference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.kakao.sdk.common.KakaoSdk

class GlobalApplication : Application() {

    companion object{
        lateinit var prefs : MyPreference
        lateinit var db : FirebaseFirestore
    }

    override fun onCreate() {
        super.onCreate()

        prefs = MyPreference(applicationContext)
        db = Firebase.firestore

        KakaoSdk.init(this,getString(R.string.kakao_native_app_key))
    }
}