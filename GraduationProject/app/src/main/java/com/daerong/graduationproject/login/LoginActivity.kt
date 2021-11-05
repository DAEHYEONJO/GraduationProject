package com.daerong.graduationproject.login

import android.app.AlertDialog
import android.content.ContentValues.TAG
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import com.daerong.graduationproject.R
import com.daerong.graduationproject.application.GlobalApplication
import com.daerong.graduationproject.data.User
import com.daerong.graduationproject.databinding.ActivityLoginBinding
import com.daerong.graduationproject.databinding.WarningDialogBinding
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.user.UserApiClient
import java.util.*

class LoginActivity : AppCompatActivity() {

    private val myPreference = GlobalApplication.prefs
    private val db = Firebase.firestore
    private val callback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
        if (error != null) {
            Log.e("KakaoLogin", "로그인 실패", error)
            Log.i("KakaoLogin", "로그인 실패 ${ error.localizedMessage}")

            if (error.localizedMessage == getString(R.string.login_fail)){
                loginWithAccount()
            }
        }
        else if (token != null) {
            Log.i("KakaoLogin", "로그인 성공 ${token.accessToken}")
            showUserInfo()

        }
    }
    private lateinit var binding : ActivityLoginBinding
    private lateinit var workSpace : String
    private var isChecked = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initSpinner()
        initBtn()

    }



    private fun initBtn() {
        binding.apply {
            loginBtn.setOnClickListener {
                if (isChecked){
                    kakaoLogin()
                }else{
                    showDiaglog()
                }
            }
//            logoutBtn.setOnClickListener {
//                kakaoLogout()
//            }
//
//            getBtn.setOnClickListener {
//                val id = myPreference.getString("id","")
//                Log.i("KakaoLogin", "curUser : $id")
//            }
//            deleteBtn.setOnClickListener {
//                myPreference.deleteString("id")
//            }
        }
    }

    private fun showDiaglog(){
        val binding = WarningDialogBinding.inflate(layoutInflater)
        val builder = AlertDialog.Builder(this@LoginActivity)
        builder.setView(binding.root)
            .setCancelable(false)
        val dialog = builder.show()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        binding.okBtn.setOnClickListener {
            dialog.dismiss()
        }
    }

    private fun initSpinner(){
        val workArray = resources.getStringArray(R.array.work_space)
        val arrayAdapter = ArrayAdapter(this@LoginActivity, R.layout.drop_down_item_login,R.id.textView, workArray)
        binding.autoCompleteTextView.setAdapter(arrayAdapter)
        binding.autoCompleteTextView.setOnItemClickListener { parent, view, position, id ->
            Log.i("click",workArray[position])
            isChecked = true
            workSpace = workArray[position]
        }
    }

    private fun kakaoLogin(){
        // 카카오톡이 설치되어 있으면 카카오톡으로 로그인, 아니면 카카오계정으로 로그인
        if (UserApiClient.instance.isKakaoTalkLoginAvailable(this@LoginActivity)) {
            UserApiClient.instance.loginWithKakaoTalk(this@LoginActivity, callback = callback)
        } else {
            loginWithAccount()
        }
    }

    private fun loginWithAccount(){
        UserApiClient.instance.loginWithKakaoAccount(this@LoginActivity, callback = callback)
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
                val userId = user.id
                var userEmail = user.kakaoAccount?.email
                val userNickName = user.kakaoAccount?.profile?.nickname
                val userProfileUrl = user.kakaoAccount?.profile?.thumbnailImageUrl
                Log.i(TAG, "사용자 정보 요청 성공" +
                        "\n회원번호: ${userId}" +
                        "\n이메일: ${userEmail}" +
                        "\n닉네임: ${userNickName}" +
                        "\n프로필사진: ${userProfileUrl}")

                if (userEmail==null){
                    userEmail = UUID.randomUUID().toString()
                }
                myPreference.setString("id",userEmail.toString())
                val curUser = myPreference.getString("id","")
                Log.i("KakaoLogin", "curUser : $curUser")

                registerDb(userEmail)
            }
        }
    }

    private fun registerDb(name : String){
        db.collection("User").document(name).set(User(name, workSpace))
    }

}