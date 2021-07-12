package com.daerong.graduationproject.login

import android.content.Context
import android.content.SharedPreferences

class MyPreference(context : Context) {
    private val prefs : SharedPreferences = context.getSharedPreferences("LOGIN",Context.MODE_PRIVATE)

    fun getString(key : String, defaultValue : String) : String{
        return prefs.getString(key,defaultValue).toString()
    }

    fun setString(key : String, value:String){
        prefs.edit().putString(key,value).apply()
    }

    fun deleteString(key : String){
        prefs.edit().remove(key).apply()
    }
}