package com.daerong.graduationproject.licenseplate

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.*
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.daerong.graduationproject.R
import com.daerong.graduationproject.adapter.PlateImgAdapter
import com.daerong.graduationproject.data.CarNumBitmap
import com.daerong.graduationproject.data.ContoursInfo
import com.daerong.graduationproject.data.PlateImgInfo
import com.daerong.graduationproject.databinding.ActivityTestGetNumBinding
import com.googlecode.tesseract.android.TessBaseAPI
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
import kotlin.math.*


class TestGetNumActivity : AppCompatActivity() {
    private var binding: ActivityTestGetNumBinding? = null
    private var imgWidth: Int = 0
    private var imgHeight: Int = 0
    private var imgChannel: Int = 0
    private lateinit var imgOrigin: Mat
    private lateinit var imgGray: Mat
    private lateinit var imgBlurred: Mat
    private lateinit var imgThresh: Mat
    private var contours = ArrayList<MatOfPoint>()
    private lateinit var hierarchy: Mat
    private var contoursInfoList = ArrayList<ContoursInfo>()
    private var possibleContoursList = HashMap<Int, ContoursInfo>()
    private var resultContourList = ArrayList<ArrayList<ContoursInfo>>()
    private var plateImgList = ArrayList<Mat>()
    private var plateImgInfo = ArrayList<PlateImgInfo>()

    private var tessBaseAPI: TessBaseAPI? = null

    private lateinit var plateImgAdapter: PlateImgAdapter

    private lateinit var mLoaderCallback: BaseLoaderCallback
    private val permissions = arrayOf(android.Manifest.permission.CAMERA)

    private var preview : Preview? = null
    private var imageCapture : ImageCapture? = null
    private lateinit var outputDirectory : File
    private lateinit var cameraExecutor : ExecutorService
    private lateinit var cameraController : CameraControl
    private lateinit var cameraInfo: CameraInfo
    private var imgIndex = 0


    companion object {
        private const val CAMERA_PERMISSION_REQUEST = 121
        private const val MIN_AREA = 80
        private const val MIN_WIDTH = 2
        private const val MIN_HEIGHT = 8
        private const val MIN_RATIO = 0.25
        private const val MAX_RATIO = 1
        private const val MAX_DIAG_MULTIPLYER =
            5//contour와 contour간 중심거리가 비교하는 contour의 대각선길이의 5배 안에 존재.
        private const val MAX_ANGLE_DIFF = 12.0//두 컨투어간 중심을 이은 각도의 최댓값
        private const val MAX_AREA_DIFF = 0.5//두 컨투어간 면적 차이
        private const val MAX_WIDTH_DIFF = 0.8//두 컨투어간 가로차이
        private const val MAX_HEIGHT_DIFF = 0.2//두 컨투어간 세로차이
        private const val MIN_N_MATCHED = 3//번호판으로 예상되는 컨투어를 뽑았는데 위의 조건이 만족되는 컨투어가 최소한 3개는 있어야함.
        private const val PLATE_WIDTH_PADDING = 1.3//번호판영역으로 측정된 가로길이 1.3배해주기
        private const val PLATE_HEIGHT_PADDING = 1.5//번호판영역으로 측정된 세로길 1.5배해주기
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("licenseplate_onResume", "onCreate ")
        super.onCreate(savedInstanceState)
        binding = ActivityTestGetNumBinding.inflate(layoutInflater)
        setContentView(binding!!.root)
        OpenCVLoader.initDebug()
        initWindow()
        initPermissions()
        initTessBaseApi()
        initRecyclerView()
        initActivityBtns()
        //initImg()
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

    }

    private fun initActivityBtns() {
        binding!!.apply {
            imgCaputreBtn.setOnClickListener {
                takePhoto()
            }
        }
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
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), { imageProxy ->
                //yuv to bitmap

                val angle = imageProxy.imageInfo.rotationDegrees

                /*imgOrigin = Mat(imageProxy.height, imageProxy.width, CvType.CV_8UC1)
                val yBuffer = imageProxy.planes[0].buffer
                val ySize = yBuffer.remaining()
                val yPlane = ByteArray(ySize)
                yBuffer[yPlane, 0, ySize]
                imgOrigin.put(0, 0, yPlane)*/

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
                rotateImage(BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size), angle.toFloat())
                initImg()


                //val bitMapGray = Bitmap.createBitmap(imgOrigin.cols(),imgOrigin.rows(),Bitmap.Config.ARGB_8888)
                //Utils.matToBitmap(imgOrigin, bitMapGray)


                imageProxy.close()
            })
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

    @SuppressLint("SimpleDateFormat")
    private fun newJpgFileName(): String {
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss")
        val filename = sdf.format(System.currentTimeMillis())
        return "${filename}_${imgIndex++}.jpg"
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        val photoFile = File(outputDirectory, newJpgFileName())
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri: Uri = Uri.fromFile(photoFile)
                    val msg = "사진캡쳐성공: $savedUri"
                    Log.d("CameraX", msg)
                    /*val angle = getRotateAngle(savedUri.path.toString())
                    val source = ImageDecoder.createSource(contentResolver, savedUri)
                    val bitmap: Bitmap = ImageDecoder.decodeBitmap(source){ imageDecoder: ImageDecoder, imageInfo: ImageDecoder.ImageInfo, source: ImageDecoder.Source ->
                        imageDecoder.isMutableRequired = true
                    }
                    rotateImage(bitmap, angle)*/
                    imgOrigin = Imgcodecs.imread(photoFile.path)
                    initImg()
                    //Glide.with(binding.root).load(savedUri).into(binding.caputedImg)
                    //initImg(savedUri.path.toString())

                }

                override fun onError(exception: ImageCaptureException) {
                    Log.d("CameraX", "사진캡쳐실패: ${exception.message}")
                }
            }
        )
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
                /*initCamera()*/
                startCameraX()
            }
        }
    }

    /*private fun initCamera() {
        mLoaderCallback = object : BaseLoaderCallback(this) {
            override fun onManagerConnected(status: Int) {
                when (status) {
                    LoaderCallbackInterface.SUCCESS -> {
                        Log.d("licenseplate_initCamera", "success called!!!")
                        binding!!.cameraView2.enableView()
                    }
                    else -> {
                        super.onManagerConnected(status)
                    }
                }
            }
        }

        binding!!.cameraView2.apply {
            setCvCameraViewListener(this@TestGetNumActivity)
            setCameraPermissionGranted()
            visibility = SurfaceView.VISIBLE
            setCameraIndex(0)
            enableView()
        }

    }*/

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

    private fun initRecyclerView() {
        plateImgAdapter = PlateImgAdapter(ArrayList<CarNumBitmap>())
        binding!!.plateImgRecyclerView.run {
            layoutManager = LinearLayoutManager(
                this@TestGetNumActivity,
                LinearLayoutManager.VERTICAL,
                false
            )
            adapter = plateImgAdapter
        }
    }

    private fun getRotateAngle(filePath: String): Float {
        val ei = ExifInterface(filePath)
        val orientation : Int = ei.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_UNDEFINED
        )
        return when(orientation){
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
    }

    private fun rotateImage(source: Bitmap, angle: Float) {
        val matrix = Matrix()
        matrix.postRotate(angle)
        imgOrigin = Mat()
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

    private fun initImg() {

        imgGray = Mat()
        imgBlurred = Mat()
        imgThresh = Mat()
        hierarchy = Mat()
        //imgOrigin = Imgcodecs.imread("/storage/self/primary/Download/600px-ROK_Vehicle_Registration_Plate_for_Private_Passenger_Car(After_Sep_2019).jpg")
        Log.d(
            "licenseplate_initImg",
            "angle : ${getRotateAngle("/storage/self/primary/Download/600px-ROK_Vehicle_Registration_Plate_for_Private_Passenger_Car(After_Sep_2019).jpg").toString()}"
        )
        //Log.d("licenseplate_initImg", getRotateAngle("/sdcard/DCIM/Camera/20210925_011604.jpg").toString())
        Log.d("licenseplate_initImg", "data addr : ${imgOrigin.dataAddr().toString()}")
        Log.d("licenseplate_initImg", "width : ${imgOrigin.width().toString()}")
        Log.d("licenseplate_initImg", "height : ${imgOrigin.height().toString()}")
        Log.d("licenseplate_initImg", "channels : ${imgOrigin.channels().toString()}")
        if (imgOrigin.dataAddr().toInt() == 0){
            Log.e("licenseplate_initImg", "img load fail")
        }else{
            imgWidth = imgOrigin.width()
            imgHeight = imgOrigin.height()
            imgChannel = imgOrigin.channels()
            //bgr to gray img
            Imgproc.cvtColor(imgOrigin, imgGray, Imgproc.COLOR_BGR2GRAY)//BGR img to GRAY img
            val bitMapGray = Bitmap.createBitmap(
                imgGray.cols(),
                imgGray.rows(),
                Bitmap.Config.ARGB_8888
            )
            Utils.matToBitmap(imgGray, bitMapGray)
            //plateImgAdapter.list.add(CarNumBitmap(bitMapGray))
            //plateImgAdapter.notifyDataSetChanged()

            //gaussian blur로 noise 제거
            val size = Size(5.0, 5.0)
            Imgproc.GaussianBlur(imgGray, imgBlurred, size, 0.0)//threshold 전 약간의 noise 제거
            val bitMapBlur = Bitmap.createBitmap(
                imgBlurred.cols(),
                imgBlurred.rows(),
                Bitmap.Config.ARGB_8888
            )
            Utils.matToBitmap(imgBlurred, bitMapBlur)
            //plateImgAdapter.list.add(CarNumBitmap(bitMapBlur))
            //plateImgAdapter.notifyDataSetChanged()

            //thresholding
            Imgproc.adaptiveThreshold(
                imgBlurred,
                imgThresh,
                255.0,
                Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                Imgproc.THRESH_BINARY_INV,
                19,
                9.0
            )//검은색과 흰색으로 이미지 나누기
            val bitMapThreshold = Bitmap.createBitmap(
                imgThresh.cols(),
                imgThresh.rows(),
                Bitmap.Config.ARGB_8888
            )
            Utils.matToBitmap(imgThresh, bitMapThreshold)
            //plateImgAdapter.list.add(CarNumBitmap(bitMapThreshold))
            //plateImgAdapter.notifyDataSetChanged()

            //contours 찾기
            Imgproc.findContours(
                imgThresh,
                contours,
                hierarchy,
                Imgproc.RETR_LIST,
                Imgproc.CHAIN_APPROX_SIMPLE
            )

            //draw contours
            val temp_result = Mat.zeros(imgHeight, imgWidth, CvType.CV_8UC1)
            Imgproc.drawContours(temp_result, contours, -1, Scalar(255.0, 255.0, 255.0))
            val bitMapContours = Bitmap.createBitmap(
                temp_result.cols(),
                temp_result.rows(),
                Bitmap.Config.ARGB_8888
            )
            Utils.matToBitmap(temp_result, bitMapContours)
            //plateImgAdapter.list.add(CarNumBitmap(bitMapContours))
            //lateImgAdapter.notifyDataSetChanged()

            //contour를 감싸는 사각형들의 좌표, 가로, 세로, 중심좌표 저장하기
            val temp_result2 = Mat.zeros(imgHeight, imgWidth, CvType.CV_8UC1)
            contours.forEach { contour->
                val rect = Imgproc.boundingRect(contour)
                Imgproc.rectangle(temp_result2, rect, Scalar(255.0, 255.0, 255.0))
                val cx = (rect.x)+(rect.width.toDouble()/2)
                val cy = (rect.y)+(rect.height.toDouble()/2)
                contoursInfoList.add(ContoursInfo(contour, rect, cx, cy))
            }
            val bitMapRectangle = Bitmap.createBitmap(
                temp_result2.cols(),
                temp_result2.rows(),
                Bitmap.Config.ARGB_8888
            )
            Utils.matToBitmap(temp_result2, bitMapRectangle)
            //plateImgAdapter.list.add(CarNumBitmap(bitMapRectangle))
            //plateImgAdapter.notifyDataSetChanged()

            //contour를 감싸는 사각형 걸러내기(번호판의 글자처럼 생긴애들만 남겨놓기)
            //1. 번호판의 각 글자들의 크기를 가정하고 최소넓이, 최소 가로세로 길이, 최소최대 (가로/세로) 비율로 번호판으로 추정되는 사각형 걸러내기
            var count = 0
            contoursInfoList.forEach {
                val area = it.rect.width * it.rect.height
                val ratio = it.rect.width.toDouble() / it.rect.height.toDouble()

                if ( (area> MIN_AREA) && (it.rect.width > MIN_WIDTH) && (it.rect.height> MIN_HEIGHT) && (ratio > MIN_RATIO) && (ratio < MAX_RATIO)) {
                    it.index = count++
                    possibleContoursList.put(it.index, it)
                }
            }
            val temp_result3 = Mat.zeros(imgHeight, imgWidth, CvType.CV_8UC1)
            possibleContoursList.values.forEach {
                val rect = it.rect
                Imgproc.rectangle(temp_result3, rect, Scalar(255.0, 255.0, 255.0))
            }
            val bitMapPossibleContours = Bitmap.createBitmap(
                temp_result3.cols(),
                temp_result3.rows(),
                Bitmap.Config.ARGB_8888
            )
            Utils.matToBitmap(temp_result3, bitMapPossibleContours)
            //plateImgAdapter.list.add(CarNumBitmap(bitMapPossibleContours))
            //plateImgAdapter.notifyDataSetChanged()

            //번호판의 숫자,문자 배열은 순차적으로 정렬되어있다. possible contours의 배열된 모양을 보고 가능성이 높은 애들을 추려내기
            val resultIndex = findFinalContour(possibleContoursList)
            val temp_result4 = Mat.zeros(imgHeight, imgWidth, CvType.CV_8UC1)
            for (i in resultIndex){
                val resultList = ArrayList<ContoursInfo>()
                for (j in i){
                    val curContour = possibleContoursList.get(j)
                    if (curContour != null) {
                        resultList.add(curContour)
                    }
                    Imgproc.rectangle(temp_result4, curContour!!.rect, Scalar(255.0, 255.0, 255.0))
                    Log.d("licenseplate_initImg", j.toString())
                }
                resultContourList.add(resultList)
                Log.d("licenseplate_initImg", "----------------")
            }
            val bitMapResultContours = Bitmap.createBitmap(
                temp_result4.cols(),
                temp_result4.rows(),
                Bitmap.Config.ARGB_8888
            )
            Utils.matToBitmap(temp_result4, bitMapResultContours)
            //plateImgAdapter.list.add(CarNumBitmap(bitMapResultContours))
            //plateImgAdapter.notifyDataSetChanged()

            rotateContours()
            lastThresholding()
        }
    }

    private fun findFinalContour(contours: HashMap<Int, ContoursInfo>) : ArrayList<ArrayList<Int>>{
        Log.d("licenseplate_initImg", "findFinalContour")
        val matchedResultIndexes = ArrayList<ArrayList<Int>>()

        for (d1 in contours.entries){
            val matchedIndexes = ArrayList<Int>()
            val unMatchedIndexes = ArrayList<Int>()
            for (d2 in contours.entries){
                if (d1.key == d2.key) continue

                val dx = abs(d1.value.cx - d2.value.cx)
                val dy = abs(d1.value.cy - d2.value.cy)
                val diagLength = hypot(
                    d1.value.rect.width.toDouble(),
                    d1.value.rect.height.toDouble()
                )
                val distance = hypot(dx, dy)
                var angleDiff = 0.0
                if (dx == 0.0){
                    //두 contour가 수직으로 배치된 경우
                    continue
                }else{
                    angleDiff = Math.toDegrees(atan(dy / dx))
                }
                val widthD1 = d1.value.rect.width.toDouble()
                val widthD2 = d2.value.rect.width.toDouble()
                val heightD1 = d1.value.rect.height.toDouble()
                val heightD2 = d2.value.rect.height.toDouble()
                val areaD1 = (widthD1 * heightD1)
                val areaD2 = (widthD2 * heightD2)
                val areaDiff = abs(areaD1 - areaD2)/areaD1
                val widthDiff = abs(widthD1 - widthD2)/widthD1
                val heightDiff = abs(heightD1 - heightD2)/heightD1

                if ( (distance < diagLength* MAX_DIAG_MULTIPLYER) && (angleDiff < MAX_ANGLE_DIFF)
                    && ( areaDiff<MAX_AREA_DIFF) && (widthDiff < MAX_WIDTH_DIFF) && (heightDiff < MAX_HEIGHT_DIFF)){
                    matchedIndexes.add(d2.key)
                }else{
                    unMatchedIndexes.add(d2.key)
                }
            }
            matchedIndexes.add(d1.key)
            if (matchedIndexes.size < MIN_N_MATCHED) continue //걸러진 contour가 3개 미만이면 버리기
            matchedResultIndexes.add(matchedIndexes)

            val unMatchedContours = HashMap<Int, ContoursInfo>()
            unMatchedIndexes.forEach {
                unMatchedContours.put(it, possibleContoursList[it]!!)
            }

            val recursiveContourList = findFinalContour(unMatchedContours)
            for (rcl in recursiveContourList){
                matchedResultIndexes.add(rcl)
            }
            break
        }
        return matchedResultIndexes
    }

    private fun rotateContours(){
        for (contour in resultContourList){
            val sortedByX = contour.sortedBy { it.rect.x }//x 좌표기준 오름차순 소팅
            val firstC = sortedByX.first()
            val lastC = sortedByX.last()
            val plateCx = (firstC.cx + lastC.cx)/2
            val plateCy = (firstC.cy + lastC.cy)/2
            val plateWidth = (lastC.rect.x.toDouble()+lastC.rect.width - firstC.rect.x) * PLATE_WIDTH_PADDING
            var heightSum = 0
            for (height in sortedByX){
                heightSum += height.rect.height
            }
            val plateHeight = heightSum.toDouble()/sortedByX.size* PLATE_HEIGHT_PADDING
            val triangleHeight = (lastC.cy - firstC.cy)
            val triangleWidth = lastC.cx - firstC.cx
            val triangleDiagonal = hypot(triangleHeight, triangleWidth)
            val angle = Math.toDegrees(asin(triangleHeight / triangleDiagonal))

            val rotationMat = Imgproc.getRotationMatrix2D(Point(plateCx, plateCy), angle, 1.0)
            val imgRotate = Mat.zeros(imgHeight, imgWidth, CvType.CV_8UC1)
            //Imgproc.warpAffine(imgThresh,imgRotate,rotationMat,Size(imgWidth.toDouble(),imgHeight.toDouble()),Imgproc.INTER_CUBIC)
            Imgproc.warpAffine(
                imgGray, imgRotate, rotationMat, Size(
                    imgWidth.toDouble(),
                    imgHeight.toDouble()
                ), Imgproc.INTER_CUBIC
            )
            val imgCropped = Mat.zeros(plateHeight.toInt(), plateWidth.toInt(), CvType.CV_8UC1)
            Imgproc.getRectSubPix(
                imgRotate,
                Size(plateWidth, plateHeight),
                Point(plateCx, plateCy),
                imgCropped
            )

            // 가로/세로 비율로 번호판 아닌것 같은것을 걸러내기
            val imgCroppedWidth = imgCropped.width().toDouble()
            val imgCroppedHeight = imgCropped.height().toDouble()
            val ratio = imgCroppedWidth/imgCroppedHeight

            if (ratio <3 || ratio > 10) continue

            plateImgList.add(imgCropped)
            plateImgInfo.add(
                PlateImgInfo(
                    x = (plateCx - plateWidth / 2).toInt(),
                    y = (plateCy - plateHeight / 2).toInt(),
                    w = plateWidth.toInt(),
                    h = plateHeight.toInt()
                )
            )

            val bitMapImgCropped = Bitmap.createBitmap(
                plateWidth.toInt(),
                plateHeight.toInt(), Bitmap.Config.ARGB_8888
            )
            Utils.matToBitmap(imgCropped, bitMapImgCropped)
            //plateImgAdapter.list.add(CarNumBitmap(bitMapImgCropped))
            //plateImgAdapter.notifyDataSetChanged()



        }
    }

    private fun lastThresholding(){
        for (plateImg in plateImgList){
            Imgproc.resize(plateImg, plateImg, Size(0.0, 0.0), 1.6, 1.6, Imgproc.INTER_CUBIC)//속도느리면 linear보간으로 바꾸기
            val bitMapImgResized = Bitmap.createBitmap(
                plateImg.width().toInt(),
                plateImg.height().toInt(), Bitmap.Config.ARGB_8888
            )
            Utils.matToBitmap(plateImg, bitMapImgResized)
            //plateImgAdapter.list.add(CarNumBitmap(bitMapImgResized,"plateImg"))
            //plateImgAdapter.notifyDataSetChanged()



            Imgproc.threshold(
                plateImg,
                plateImg,
                0.0,
                255.0,
                Imgproc.THRESH_BINARY_INV or Imgproc.THRESH_OTSU
            )
            val bitMapImgThresh = Bitmap.createBitmap(
                plateImg.width().toInt(),
                plateImg.height().toInt(), Bitmap.Config.ARGB_8888
            )
            Utils.matToBitmap(plateImg, bitMapImgThresh)
            //plateImgAdapter.list.add(CarNumBitmap(bitMapImgThresh))
            //plateImgAdapter.notifyDataSetChanged()



            val contours = ArrayList<MatOfPoint>()
            Imgproc.findContours(
                plateImg,
                contours,
                hierarchy,
                Imgproc.RETR_LIST,
                Imgproc.CHAIN_APPROX_SIMPLE
            )

            var plateMinX = plateImg.width()
            var plateMinY = plateImg.height()
            var plateMaxX = 0
            var plateMaxY = 0

            for(contour in contours){
                val rect = Imgproc.boundingRect(contour)
                val area = rect.width * rect.height
                val ratio = rect.width.toDouble() / rect.height.toDouble()
                if ((area > MIN_AREA) && (rect.width > MIN_WIDTH) && (rect.height > MIN_HEIGHT) && (ratio > MIN_RATIO) && (ratio < MAX_RATIO)){
                    if (rect.x < plateMinX) plateMinX = rect.x
                    if (rect.y < plateMinY) plateMinY = rect.y
                    if ((rect.x + rect.width) > plateMaxX) plateMaxX = rect.x + rect.width
                    if ((rect.y + rect.height) > plateMaxY) plateMaxY = rect.y + rect.height
                }

            }

            if (plateMinX == plateImg.width() && plateMinY == plateImg.height()) continue

            val rectROI = Rect(plateMinX, plateMinY, plateMaxX - plateMinX, plateMaxY - plateMinY)
            Log.d(
                "plateLogtlqkf",
                "platewidth : ${plateImg.width()} plateheight : ${plateImg.height()} plateMinX : ${plateMinX} plateMinY : ${plateMinY} plateMaxX : $plateMaxX plateMaxY : $plateMaxY"
            )
            //val rectROI = Rect(plateMinY,plateMinX,plateMaxY-plateMinY,plateMaxX-plateMinX)
            val imgResult = plateImg.submat(rectROI)
            val bitMapImgResult = Bitmap.createBitmap(
                imgResult.width().toInt(),
                imgResult.height().toInt(), Bitmap.Config.ARGB_8888
            )
            Utils.matToBitmap(imgResult, bitMapImgResult)

            tessBaseAPI?.run {
                setImage(bitMapImgResult)
                plateImgAdapter.list.add(CarNumBitmap(bitMapImgResult, utF8Text + " imgsubmat"))
                plateImgAdapter.notifyDataSetChanged()
                Log.d("licenseplate_carnum", "carnum : ${(utF8Text)}")
                if (isCorrectNum(getOcrString(utF8Text)))
                    Log.d("licenseplate_initImg", "img resized : ${getOcrString(utF8Text)}")
            }

            Imgproc.GaussianBlur(imgResult, imgResult, Size(3.0, 3.0), 0.0)
            Imgproc.threshold(
                imgResult,
                imgResult,
                0.0,
                255.0,
                Imgproc.THRESH_BINARY or Imgproc.THRESH_OTSU
            )

            val last2 = Bitmap.createBitmap(
                imgResult.width().toInt(),
                imgResult.height().toInt(), Bitmap.Config.ARGB_8888
            )
            Utils.matToBitmap(imgResult, last2)
            tessBaseAPI?.run {
                setImage(last2)
                Log.d("licenseplate_carnum", "carnum : ${(utF8Text)}")
                plateImgAdapter.list.add(CarNumBitmap(last2, utF8Text + " imgsubmat"))
                plateImgAdapter.notifyDataSetChanged()
                if (isCorrectNum(getOcrString(utF8Text)))
                    Log.d("licenseplate_initImg", "img gaussian+thresh : ${getOcrString(utF8Text)}")
            }

            Core.copyMakeBorder(
                imgResult, imgResult, 5, 5, 5, 5, Core.BORDER_CONSTANT, Scalar(
                    0.0,
                    0.0,
                    0.0
                )
            )

            val last = Bitmap.createBitmap(
                imgResult.width().toInt(),
                imgResult.height().toInt(), Bitmap.Config.ARGB_8888
            )
            Utils.matToBitmap(imgResult, last)

            tessBaseAPI?.run {
                setImage(last)
                Log.d("licenseplate_carnum", "carnum : ${(utF8Text)}")
                plateImgAdapter.list.add(CarNumBitmap(last, utF8Text + " imgsubmat"))
                plateImgAdapter.notifyDataSetChanged()
                if (isCorrectNum(getOcrString(utF8Text)))
                    Log.d("licenseplate_initImg", "img last : ${getOcrString(utF8Text)}")
            }
        }
    }

    private fun getOcrString(str: String): String {
        val regex = """[^가-힣0-9]""".toRegex()
        val result = str.replace(regex, "")
        var startIndex = 0
        for ((i, c) in result.withIndex()) {
            if (c.isDigit()) {
                startIndex = i
                break
            }
        }
        var lastIndex = result.length - 1
        for (i in result.length - 1 downTo startIndex + 1) {
            if (result[i].isDigit()) {
                lastIndex = i
                break
            }
        }
        return result.substring(startIndex, lastIndex + 1)
    }

    private fun isCorrectNum(str: String): Boolean {
        var korCount = 0
        if (str.length in 7..8) {
            str.forEach {
                if (!it.isLetterOrDigit()) return false
                if (it.isLetter()) korCount++
                if (korCount > 1) return false
            }
        } else {
            return false
        }
        return true
    }

    override fun onResume() {
        super.onResume()
        Log.d("licenseplate_onResume", "onResume ")
        /*if (!OpenCVLoader.initDebug()) {
            Log.d("licenseplate_onResume", "onResume : OpenCV initialization error")
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback)
        } else {
            Log.d("licenseplate_onResume", "onResume : OpenCV initialization success")
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        }*/
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
                }
            }
        }
    }

    /*override fun onCameraViewStarted(width: Int, height: Int) {
        Log.d("onCameraFrame", "onCameraViewStarted")
    }

    override fun onCameraViewStopped() {
        Log.d("onCameraFrame", "onCameraViewStopped")
    }

    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame?): Mat {
        Log.d("onCameraFrame", "onCameraFrameCalled")
        CoroutineScope(Dispatchers.Main).launch {
            initImg(inputFrame!!.rgba())
        }
        return inputFrame!!.rgba()
    }*/



    fun rotate(src: Mat, angle: Double): Mat{
        val rotate = Mat(src.size(), CvType.CV_8UC1)
        val matrix = Imgproc.getRotationMatrix2D(
            Point(
                src.width() / 2.toDouble(),
                src.height() / 2.toDouble()
            ), angle, 1.0
        )
        Imgproc.warpAffine(src, rotate, matrix, src.size(), Imgproc.INTER_LINEAR)
        return rotate
    }


    override fun onPause() {
        super.onPause()
        /*binding?.cameraView2?.disableView()*/
    }

    override fun onDestroy() {
        super.onDestroy()
        /*binding?.cameraView2?.disableView()
        binding = null*/
    }


}