package com.daerong.graduationproject.application

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.app.Dialog
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.ColorDrawable
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatDialog
import com.bumptech.glide.Glide
import com.daerong.graduationproject.R
import com.daerong.graduationproject.databinding.LoadingDialogBinding
import com.daerong.graduationproject.login.MyPreference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.kakao.sdk.common.KakaoSdk

class GlobalApplication : Application() {

    companion object{
        lateinit var prefs : MyPreference
        private lateinit var globalApplication: GlobalApplication
        fun getInstance() : GlobalApplication {
            return globalApplication
        }
    }
    private var progressDialog: AppCompatDialog? = null

    override fun onCreate() {
        super.onCreate()

        prefs = MyPreference(applicationContext)
        globalApplication = this
        KakaoSdk.init(this,getString(R.string.kakao_native_app_key))
    }



    fun progressOn(activity : Activity){
        if (activity == null || activity.isFinishing){
            return
        }

        val binding = LoadingDialogBinding.inflate(activity.layoutInflater)
        val anim = AnimationUtils.loadAnimation(activity,R.anim.loading)
        binding.image.animation = anim
        //Glide.with(binding.root).load(R.drawable.ic_baseline_photo_camera_24).into(binding.loadingImgView)
        // Glide.with(binding.root).asGif().load(R.drawable.loading).into(binding.loadingImgView)
        //binding.loadingImgView.setImageResource(R.drawable.loading)
        progressDialog = AppCompatDialog(activity)
        progressDialog!!.run {
            setCancelable(false)
            window?.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
            setContentView(binding.root)
            show()
        }



    }

    fun progressOff(){
        if(progressDialog!=null && (progressDialog?.isShowing == true)){
            progressDialog?.dismiss()
        }
    }
}