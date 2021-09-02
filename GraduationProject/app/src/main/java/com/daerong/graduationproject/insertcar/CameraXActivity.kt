package com.daerong.graduationproject.insertcar

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.hardware.Camera.open
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.camera.core.*
import androidx.camera.core.impl.ImageCaptureConfig
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.CameraController
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
import kotlinx.coroutines.Job
import org.opencv.videoio.VideoCapture
import java.io.File
import java.nio.channels.AsynchronousFileChannel.open
import java.text.SimpleDateFormat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraXActivity : AppCompatActivity() {
    lateinit var binding : ActivityCameraXBinding

    private var preview : Preview? = null
    private var imageCapture : ImageCapture? = null

    private lateinit var outputDirectory : File
    private lateinit var cameraExecutor : ExecutorService
    private lateinit var cameraController : CameraControl
    private lateinit var cameraInfo: CameraInfo

    private lateinit var cameraImgAdapter: CameraImgAdapter
    private var imgIndex = 0
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
        Log.d("testBtn",outputDirectory.toString())
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun initBtn() {
        binding.apply {
            completeBtn.setOnClickListener {
                val checkedList = ArrayList<File>()
                cameraImgAdapter.imgList.forEachIndexed { index, cameraX ->
                    if (cameraX.checked) {
                        Log.d("photoChecked","${index} : ${cameraX.file}")
                        checkedList.add(cameraX.file)
                    }
                    else Log.d("photoChecked","not checked ${index} : ${cameraX.file}")
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
            flashBtn.setOnClickListener {
                when(cameraInfo.torchState.value){
                    TorchState.ON->{
                        cameraController.enableTorch(false)
                    }
                    TorchState.OFF->{
                        cameraController.enableTorch(true)
                    }
                }
            }
            viewFinder.setOnTouchListener { v, event ->
                when(event.action){
                    MotionEvent.ACTION_DOWN->{
                        v.performClick()
                        return@setOnTouchListener true
                    }
                    MotionEvent.ACTION_UP->{
                        val factory = this.viewFinder.meteringPointFactory
                        val point = factory.createPoint(event.x,event.y)
                        val action = FocusMeteringAction.Builder(point).build()
                        cameraController.startFocusAndMetering(action)
                        Log.d("viewFinderMotion", "x : ${event.x} y : ${event.y}")
                        v.performClick()
                        return@setOnTouchListener true
                    }
                    else->return@setOnTouchListener false
                }
            }

        }
    }

    private fun initCameraXAdapter(){
        cameraImgAdapter = CameraImgAdapter(ArrayList<CameraX>(), binding)
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
                    val cameraX = CameraX(photoFile)
                    cameraImgAdapter.imgList.add(cameraX)
                    cameraImgAdapter.notifyItemInserted(cameraImgAdapter.itemCount-1)
                    binding.imgRecyclerView.scrollToPosition(cameraImgAdapter.itemCount-1)
                    binding.imgRecyclerView.visibility = View.VISIBLE
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
        return "${filename}_${imgIndex++}.jpg"
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
                cameraController = camera.cameraControl
                cameraInfo = camera.cameraInfo

            }catch (e : Exception){
                Log.e("CameraX",e.toString())
            }
        }, ContextCompat.getMainExecutor(this))
    }
}