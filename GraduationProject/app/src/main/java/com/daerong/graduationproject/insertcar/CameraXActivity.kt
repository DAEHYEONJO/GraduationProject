package com.daerong.graduationproject.insertcar

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.impl.ImageCaptureConfig
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.daerong.graduationproject.R
import com.daerong.graduationproject.adapter.CameraImgAdapter
import com.daerong.graduationproject.data.CameraX
import com.daerong.graduationproject.databinding.ActivityCameraXBinding
import com.daerong.graduationproject.viewmodel.CameraXViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraXActivity : AppCompatActivity() {
    lateinit var binding : ActivityCameraXBinding

    private var preview : Preview? = null
    private var imageCapture : ImageCapture? = null

    private lateinit var outputDirectory : File
    private lateinit var cameraExecutor : ExecutorService

    private lateinit var cameraImgAdapter: CameraImgAdapter
    private val cameraXViewModel : CameraXViewModel by viewModels<CameraXViewModel>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val actionBar = supportActionBar
        actionBar?.hide()
        
        binding = ActivityCameraXBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initBtn()
        initCameraXAdapter()
        startCamera()
        binding.imgCaputreBtn.setOnClickListener {
            takePhoto()
        }
        outputDirectory = getOutputDirectory()
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun initBtn() {
        binding.apply {
            completeBtn.setOnClickListener {
                val checkedList = ArrayList<String>()
                cameraImgAdapter.imgList.forEachIndexed { index, cameraX ->
                    if (cameraX.checked) {
                        Log.d("photoChecked","${index} : ${cameraX.uri}")
                        checkedList.add(cameraX.uri.toString())
                    }
                    else Log.d("photoChecked","not checked ${index} : ${cameraX.uri}")
                }
                Log.d("photoChecked","backBtn clicked2")
                if (checkedList.size!=0){
                    val intent = Intent()
                    intent.putExtra("imgCheckedList",checkedList)
                    setResult(Activity.RESULT_OK,intent)
                }else{
                    Log.d("photoChecked","체크된 사진 없음 그냥 종료")
                }
                finish()
            }
        }
    }

    private fun initCameraXAdapter(){
        cameraImgAdapter = CameraImgAdapter(ArrayList<CameraX>(), this)
        cameraImgAdapter.listener = object : CameraImgAdapter.OnImageClickListener{
            override fun onClick(cameraX: CameraX) {

            }
        }
        binding.imgRecyclerView.run {
            layoutManager = LinearLayoutManager(this@CameraXActivity,LinearLayoutManager.HORIZONTAL,false)
            adapter = cameraImgAdapter
        }
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull().let {it->
            File(it, resources.getString(R.string.app_name)).apply {
                mkdirs()
            }
        }
        return if (mediaDir != null && mediaDir.exists()) mediaDir
        else filesDir
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        val photoFile = File(outputDirectory, newJpgFileName())
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback{
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri : Uri = Uri.fromFile(photoFile)
                    val msg = "사진캡쳐성공: $savedUri"
                    //Glide.with(binding.root).load(savedUri).into(binding.caputedImg)
                    val cameraX = CameraX(savedUri)
                    cameraImgAdapter.imgList.add(cameraX)
                    cameraImgAdapter.notifyItemInserted(cameraImgAdapter.itemCount-1)
                    binding.imgRecyclerView.scrollToPosition(cameraImgAdapter.itemCount-1)
                    Toast.makeText(this@CameraXActivity, msg, Toast.LENGTH_SHORT).show()
                    Log.d("CameraX", msg)
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.d("CameraX", "사진캡쳐실패: ${exception.message}")
                }
            }
        )
    }

    @SuppressLint("SimpleDateFormat")
    private fun newJpgFileName(): String {
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss")
        val filename = sdf.format(System.currentTimeMillis())
        return "${filename}.jpg"
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider : ProcessCameraProvider = cameraProviderFuture.get()
            preview  = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }
            imageCapture = ImageCapture.Builder().build()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    this.preview,
                    imageCapture
                )
                val cameraController = camera.cameraControl

            }catch (e : Exception){
                Log.e("CameraX",e.toString())
            }
        }, ContextCompat.getMainExecutor(this))
    }
}