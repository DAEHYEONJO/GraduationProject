package com.daerong.graduationproject.login

import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Base64
import android.util.Log
import com.daerong.graduationproject.R
import com.daerong.graduationproject.application.GlobalApplication
import com.daerong.graduationproject.databinding.ActivityLoginBinding
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.user.UserApiClient
import com.minew.device.baseblelibaray.a.e
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

class LoginActivity : AppCompatActivity() {

    private val myPreference = GlobalApplication.prefs
    private val callback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
        if (error != null) {
            Log.e("KakaoLogin", "로그인 실패", error)
        }
        else if (token != null) {
            Log.i("KakaoLogin", "로그인 성공 ${token.accessToken}")
            showUserInfo()

        }
    }
    private lateinit var binding : ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initBtn()

    }

    private fun initBtn() {
        binding.apply {
            loginBtn.setOnClickListener {
                kakaoLogin()
            }
            logoutBtn.setOnClickListener {
                kakaoLogout()
            }

            getBtn.setOnClickListener {
                val id = myPreference.getString("id","")
                Log.i("KakaoLogin", "curUser : $id")
            }
            deleteBtn.setOnClickListener {
                myPreference.deleteString("id")
            }
        }
    }

    private fun kakaoLogin(){
        // 카카오톡이 설치되어 있으면 카카오톡으로 로그인, 아니면 카카오계정으로 로그인
        if (UserApiClient.instance.isKakaoTalkLoginAvailable(this@LoginActivity)) {
            UserApiClient.instance.loginWithKakaoTalk(this@LoginActivity, callback = callback)
        } else {
            UserApiClient.instance.loginWithKakaoAccount(this@LoginActivity, callback = callback)
        }
    }

    private fun kakaoLogout(){
        UserApiClient.instance.unlink { error ->
            if (error != null) {
                Log.e(TAG, "연결 끊기 실패", error)
            }
            else {
                Log.i(TAG, "연결 끊기 성공. SDK에서 토큰 삭제 됨")
                val curUser = myPreference.getString("id","")
                Log.i("KakaoLogin", "curUser : $curUser")
                myPreference.deleteString("id")
                val curUser2 = myPreference.getString("id","")
                Log.i("KakaoLogin", "curUser : $curUser2")
            }
        }
    }

    private fun showUserInfo(){
        UserApiClient.instance.me { user, error ->
            if (error != null) {
                Log.e(TAG, "사용자 정보 요청 실패", error)
            }
            else if (user != null) {
                Log.i(TAG, "사용자 정보 요청 성공" +
                        "\n회원번호: ${user.id}" +
                        "\n이메일: ${user.kakaoAccount?.email}" +
                        "\n닉네임: ${user.kakaoAccount?.profile?.nickname}" +
                        "\n프로필사진: ${user.kakaoAccount?.profile?.thumbnailImageUrl}")
                myPreference.setString("id",user.kakaoAccount?.email.toString())
                val curUser = myPreference.getString("id","")
                Log.i("KakaoLogin", "curUser : $curUser")
            }
        }
    }

}