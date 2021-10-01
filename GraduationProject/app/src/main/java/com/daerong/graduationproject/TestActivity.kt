package com.daerong.graduationproject

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.daerong.graduationproject.databinding.ActivityTestBinding
import com.googlecode.tesseract.android.TessBaseAPI
import kotlinx.coroutines.*
import java.io.*

var tessBaseAPI: TessBaseAPI? = null

class TestActivity : AppCompatActivity() {
    lateinit var binding : ActivityTestBinding
    var scope = CoroutineScope(Dispatchers.IO)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initTessBaseApi()
        CoroutineScope(Dispatchers.Main).launch {
            Log.d("coroutine","hi1")

            val text : String? = scope.async {
                tessBaseAPI?.run {
                    setImage(File("/storage/emulated/0/Download/Korean_License_Plate_for_Rent_Passenger_car_-_Heo.jpg"))
                    utF8Text
                }
            }.await()
            Log.d("coroutine",text!!)
        }
        Log.d("coroutine","hi3")
    }

    private fun initTessBaseApi() {
        tessBaseAPI = TessBaseAPI()
        val dir = "$filesDir/tesseract"
        if (checkLanguageFile("$dir/tessdata")) {
            tessBaseAPI!!.init(dir, "kor", TessBaseAPI.OEM_TESSERACT_ONLY)
            tessBaseAPI!!.pageSegMode = TessBaseAPI.PageSegMode.PSM_SINGLE_LINE
        }
    }

    fun checkLanguageFile(dir: String): Boolean {
        val file = File(dir)
        if (!file.exists() && file.mkdirs()) createFiles(dir) else if (file.exists()) {
            val filePath = "$dir/kor.traineddata"
            val langDataFile = File(filePath)
            if (!langDataFile.exists()) createFiles(dir)
        }
        return true
    }

    private fun createFiles(dir: String) {
        val assetMgr = this.assets
        var inputStream: InputStream? = null
        var outputStream: OutputStream? = null
        try {
            inputStream = assetMgr.open("kor.traineddata")
            val destFile = "$dir/kor.traineddata"
            outputStream = FileOutputStream(destFile)
            val buffer = ByteArray(1024)
            var read: Int
            while (inputStream.read(buffer).also { read = it } != -1) {
                outputStream.write(buffer, 0, read)
            }
            inputStream.close()
            outputStream.flush()
            outputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}