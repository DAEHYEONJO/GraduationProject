package com.daerong.graduationproject.application

import android.app.Application
import com.daerong.graduationproject.R
import com.daerong.graduationproject.login.MyPreference
import com.kakao.sdk.common.KakaoSdk

class GlobalApplication : Application() {

    companion object{
        lateinit var prefs : MyPreference
    }

    override fun onCreate() {
        super.onCreate()

        prefs = MyPreference(applicationContext)

        KakaoSdk.init(this,getString(R.string.kakao_native_app_key))
    }
}