package com.daerong.graduationproject.licenseplate

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.daerong.graduationproject.R
import com.daerong.graduationproject.adapter.CarNumResultAdapter
import com.daerong.graduationproject.adapter.PlateImgAdapter
import com.daerong.graduationproject.data.CarNumBitmap
import com.daerong.graduationproject.data.ContoursInfo
import com.daerong.graduationproject.data.PlateImgInfo
import com.daerong.graduationproject.databinding.ActivityTestGetNumBinding
import com.googlecode.tesseract.android.TessBaseAPI
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.opencv.android.*
import org.opencv.core.*
import org.opencv.core.Point
import org.opencv.core.Rect
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import java.io.*
import java.text.SimpleDateFormat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.*


class TestGetNumActivity : AppCompatActivity() {
    private var binding: ActivityTestGetNumBinding? = null
    private lateinit var imgOrigin: Mat

    private var tessBaseAPI: TessBaseAPI? = null

    private lateinit var carNumResultAdapter: CarNumResultAdapter
    private val permissions = arrayOf(android.Manifest.permission.CAMERA)

    private var preview : Preview? = null
    private var imageCapture : ImageCapture? = null
    private lateinit var outputDirectory : File
    private lateinit var cameraExecutor : ExecutorService
    private lateinit var cameraController : CameraControl
    private lateinit var cameraInfo: CameraInfo
    private lateinit var imageAnalysis: ImageAnalysis
    val mutex = Mutex()
    private var resultCarNum : String? = null


    companion object {
        private const val CAMERA_PERMISSION_REQUEST = 121
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("licenseplate_onResume", "onCreate ")
        super.onCreate(savedInstanceState)
        binding = ActivityTestGetNumBinding.inflate(layoutInflater)
        setContentView(binding!!.root)
        OpenCVLoader.initDebug()
        initWindow()
        initBtns()
        initRecyclerView()
        initPermissions()
        initTessBaseApi()
    }

    private fun initBtns() {
        binding!!.run {
            flashOnOffBtn.setOnCheckedChangeListener { buttonView, isChecked ->
                if (isChecked){
                    cameraController.enableTorch(true)
                }else{
                    cameraController.enableTorch(false)
                }
            }
            cameraOrTextBtn.setOnCheckedChangeListener { buttonView, isChecked ->
                if (isChecked){
                    mainLayout.visibility = View.GONE
                    keyboardInputLayout.visibility = View.VISIBLE
                    imageAnalysis.clearAnalyzer()
                    showSoftInput()
                }else{
                    keyboardInputLayout.visibility = View.GONE
                    mainLayout.visibility = View.VISIBLE
                    setImageAnalysis()
                }
            }
            keyboardInputOkBtn.setOnClickListener {
                if (isCorrectNum(keyboardInputText.text.toString())){
                    intent.putExtra("carNumResult",keyboardInputText.text.toString())
                    setResult(1012,intent)
                    finish()
                }
            }
        }
    }

    private fun initRecyclerView() {
        carNumResultAdapter = CarNumResultAdapter(ArrayList<String>())
        binding!!.carNumRecyclerView.run {
            adapter = carNumResultAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL,false)
        }
    }

    private fun initWindow() {
        val actionBar = supportActionBar
        actionBar?.hide()
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        window.setFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)

    }

    private fun showSoftInput() {
        binding!!.keyboardInputText.requestFocus()
        val inputMethodManager =
            getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.showSoftInput(binding!!.keyboardInputText, 0)
    }

    @SuppressLint("UnsafeOptInUsageError", "RestrictedApi")
    private fun startCameraX() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding!!.viewFinder.surfaceProvider)
                }
            outputDirectory = getOutputDirectory()
            Log.d("testBtn", outputDirectory.toString())
            imageCapture = ImageCapture.Builder().build()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            setImageAnalysis()


            try {
                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    this.preview,
                    imageCapture,
                    imageAnalysis
                )
                cameraController = camera.cameraControl
                cameraInfo = camera.cameraInfo


            } catch (e: Exception) {
                Log.e("CameraX", e.toString())
            }
        }, ContextCompat.getMainExecutor(this))

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun setImageAnalysis(){
        var lastAnalysisTime = 0L
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this@TestGetNumActivity), { imageProxy ->
            /*CoroutineScope(Dispatchers.Main).launch {
                var curAnalysisTime = System.currentTimeMillis()
                if (curAnalysisTime - lastAnalysisTime >= TimeUnit.SECONDS.toMillis(1)){//1초주기마다 콜백받기
                    Log.d("imageAnalysis","cur :  ${curAnalysisTime.toString()}")
                    //yuv to bitmap

                    getOriginImage(imageProxy)

                    val curDeffered = CoroutineScope(Dispatchers.Default).async (CoroutineName("$curAnalysisTime")) {
                        mutex.withLock {
                            CarPlate(imgOrigin, tessBaseAPI = tessBaseAPI, Thread.currentThread().id).initImg()
                        }
                    }

                    this.launch {
                        val carPlateSet = curDeffered.await()
                        if (carPlateSet?.size!=0 && carPlateSet!=null){
                            imageAnalysis.clearAnalyzer()
                            carNumResultAdapter.carNumList = ArrayList<String>(carPlateSet)
                            binding!!.run {
                                customViewRect.visibility = View.GONE
                                resultLayout.visibility = View.VISIBLE
                                carNumResultAdapter.notifyDataSetChanged()
                                restartLayout.setOnClickListener {
                                    setImageAnalysis()
                                    resultLayout.visibility = View.GONE
                                    customViewRect.visibility = View.VISIBLE
                                }
                            }
                        }
                    }


                    //위의 coroutine 으로부터 return 된 HashSet<String>객체의 size로 UI Update여부 판단하기
                    //현재까지

                    lastAnalysisTime = curAnalysisTime

                }
                imageProxy.close()
            }//코루틴 Main 괄호*/
            var curAnalysisTime = System.currentTimeMillis()
            if (curAnalysisTime - lastAnalysisTime >= TimeUnit.SECONDS.toMillis(1)){//1초주기마다 콜백받기
                Log.d("imageAnalysis","cur :  ${curAnalysisTime.toString()}")
                //yuv to bitmap

                getOriginImage(imageProxy)

                CoroutineScope(Dispatchers.Default).launch  {
                    mutex.withLock {
                        val carPlateSet = CarPlate(imgOrigin, tessBaseAPI = tessBaseAPI, Thread.currentThread().id).initImg()
                        withContext(Dispatchers.Main){
                            if (carPlateSet.size !=0){
                                imageAnalysis.clearAnalyzer()
                                carNumResultAdapter.carNumList = ArrayList<String>(carPlateSet)
                                binding!!.run {
                                    customViewRect.visibility = View.GONE
                                    resultLayout.visibility = View.VISIBLE
                                    carNumResultAdapter.run {
                                        notifyDataSetChanged()
                                        setOnItemClickListener(object : CarNumResultAdapter.OnItemClickListener{
                                            override fun onClick(carNum: String) {
                                                Log.d("selected_car_num",carNum)

                                                intent.putExtra("carNumResult",carNum)
                                                setResult(1012,intent)
                                                finish()

                                            }
                                        })
                                    }
                                    restartLayout.setOnClickListener {
                                        setImageAnalysis()
                                        resultLayout.visibility = View.GONE
                                        customViewRect.visibility = View.VISIBLE
                                    }
                                }
                            }
                        }
                    }
                }


                //위의 coroutine 으로부터 return 된 HashSet<String>객체의 size로 UI Update여부 판단하기
                //현재까지

                lastAnalysisTime = curAnalysisTime

            }
            imageProxy.close()
        })
    }

    private fun getOriginImage(imageProxy: ImageProxy){
        imageProxy.setCropRect(android.graphics.Rect(100, 300, 700, 100))
        val angle = imageProxy.imageInfo.rotationDegrees
        val yBuffer = imageProxy.planes[0].buffer // Y
        val vuBuffer = imageProxy.planes[2].buffer // VU
        val ySize = yBuffer.remaining()
        val vuSize = vuBuffer.remaining()
        val nv21 = ByteArray(ySize + vuSize)
        yBuffer.get(nv21, 0, ySize)
        vuBuffer.get(nv21, ySize, vuSize)
        val yuvImage = YuvImage(nv21, ImageFormat.NV21, imageProxy.width, imageProxy.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(android.graphics.Rect(0, 0, yuvImage.width, yuvImage.height), 50, out)
        val imageBytes = out.toByteArray()
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        rotateImage(bitmap, angle.toFloat())
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull().let { it->
            File(it, resources.getString(R.string.app_name)).apply {
                mkdirs()
            }
        }
        return if (mediaDir != null && mediaDir.exists()) mediaDir
        else filesDir
    }

    private fun initPermissions() {
        permissions.forEach {
            if (ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, permissions, CAMERA_PERMISSION_REQUEST)
            } else {
                Toast.makeText(this, "카메라 권한 수락", Toast.LENGTH_SHORT).show()
                startCameraX()
            }
        }
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


    private fun rotateImage(source: Bitmap, angle: Float){
        val matrix = Matrix()
        matrix.postRotate(angle)
        imgOrigin = Mat()
        //test용
        Utils.bitmapToMat(
            Bitmap.createBitmap(
                source,
                0,
                0,
                source.width,
                source.height,
                matrix,
                true
            ), imgOrigin
        )
    }



    override fun onResume() {
        super.onResume()
        Log.d("licenseplate_onResume", "onResume ")
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            CAMERA_PERMISSION_REQUEST -> {
                var flag = true
                for (granted in grantResults) {
                    if (granted != PackageManager.PERMISSION_GRANTED) {
                        initPermissions()
                        flag = false
                    }
                }
                if (flag) {
                    Toast.makeText(this, "카메라 권한 허용", Toast.LENGTH_SHORT).show()
                    startCameraX()
                }
            }
        }
    }

    private fun isCorrectNum(str: String): Boolean {
        var korCount = 0
        var korIndex = 0
        //숫자 2개 or 3개, 한글, 숫자 4개 조합인지 확인하기
        if (str.length in 7..8) {
            for ((i,c) in str.withIndex()){
                if (!c.isLetterOrDigit()) return false
                if (c.isLetter()) {
                    korCount++
                    korIndex = i
                }
            }
            if (korCount!=1) return false
            if (str.substring(korIndex,str.length-1).length!=4) return false
        } else {
            return false
        }
        return true
    }


    override fun onPause() {
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
    }



}