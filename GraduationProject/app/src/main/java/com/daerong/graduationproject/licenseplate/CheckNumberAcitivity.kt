package com.daerong.graduationproject.licenseplate

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.*
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.os.*
import android.util.Log
import android.util.Size
import android.util.SparseIntArray
import android.view.Surface
import android.view.TextureView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.daerong.graduationproject.databinding.ActivityCheckNumberBinding
import com.googlecode.tesseract.android.TessBaseAPI
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc
import java.io.*
import java.util.*


class CheckNumberAcitivity : AppCompatActivity() {
    lateinit var binding: ActivityCheckNumberBinding
    var tessBaseAPI: TessBaseAPI? = null

    private var cameraId: String? = null
    protected var cameraDevice: CameraDevice? = null
    protected var cameraCaptureSessions: CameraCaptureSession? = null
    private var mBackgroundHandler: Handler? = null
    private var mBackgroundThread: HandlerThread? = null

    protected var captureRequestBuilder: CaptureRequest.Builder? = null
    private var imageDimension: Size? = null
    private var imageReader: ImageReader? = null

    var imgBase: Bitmap? = null
    var roi: Bitmap? = null

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 200
        private val ORIENTATIONS = SparseIntArray()

        @Synchronized
        fun GetRotatedBitmap(bitmap: Bitmap?, degrees: Int): Bitmap? {
            var bitmap = bitmap
            if (degrees != 0 && bitmap != null) {
                val m = Matrix()
                m.setRotate(
                        degrees.toFloat(),
                        bitmap.width.toFloat() / 2,
                        bitmap.height.toFloat() / 2
                )
                try {
                    val b2 = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, m, true)
                    if (bitmap != b2) {
                        bitmap = b2
                    }
                } catch (ex: OutOfMemoryError) {
                    ex.printStackTrace()
                }
            }
            return bitmap
        }
        init {
            ORIENTATIONS.append(Surface.ROTATION_0, 90)
            ORIENTATIONS.append(Surface.ROTATION_90, 0)
            ORIENTATIONS.append(Surface.ROTATION_180, 270)
            ORIENTATIONS.append(Surface.ROTATION_270, 180)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.e("rongmera", "onCreate")
        binding = ActivityCheckNumberBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initView()
        tessBaseAPI = TessBaseAPI()
        val dir = "$filesDir/tesseract"
        if (checkLanguageFile("$dir/tessdata")) tessBaseAPI!!.init(dir, "kor")
    }


    private fun initView(){
        binding.textureView.surfaceTextureListener = textureListener
        binding.takePictureBtn.setOnClickListener { takePicture() }
    }

    private fun takePicture(){
        val manager = getSystemService(CAMERA_SERVICE) as CameraManager
        try {
            val characteristics = manager.getCameraCharacteristics(
                    cameraDevice!!.id
            )
            var jpegSizes: Array<Size>? = null
            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            jpegSizes = map!!.getOutputSizes(ImageFormat.JPEG)
            var width = 640
            var height = 480
            if (jpegSizes != null && 0 < jpegSizes.size) {
                width = jpegSizes[0].width
                height = jpegSizes[0].height
            }
            val imageReader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1)
            val outputSurfaces: MutableList<Surface> = ArrayList(2)
            outputSurfaces.add(imageReader.surface)
            outputSurfaces.add(Surface(binding.textureView.surfaceTexture))
            val captureBuilder =
                cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            captureBuilder.addTarget(imageReader.surface)
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
            val rotation = windowManager.defaultDisplay.rotation
            captureBuilder.set(
                    CaptureRequest.JPEG_ORIENTATION,
                    ORIENTATIONS[rotation]
            )
            val readerListener =
                ImageReader.OnImageAvailableListener { reader ->
                    var image: Image? = null
                    try {
                        image = reader.acquireLatestImage()
                        val buffer = image.planes[0].buffer
                        val bytes = ByteArray(buffer.capacity())
                        buffer[bytes]
                        Log.d("rongmera", "takePicture")
                        val options = BitmapFactory.Options()
                        options.inSampleSize = 8
                        var bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        bitmap = GetRotatedBitmap(bitmap, 90)
                        val imgRoi: Bitmap

                        OpenCVLoader.initDebug() // 초기화
                        val matBase = Mat()
                        Utils.bitmapToMat(bitmap, matBase)
                        val matGray = Mat()
                        val matCny = Mat()
                        Imgproc.cvtColor(matBase, matGray, Imgproc.COLOR_BGR2GRAY)
                        Imgproc.Canny(matGray, matCny, 10.0, 100.0, 3, true)
                        Imgproc.threshold(matGray, matCny, 150.0, 255.0, Imgproc.THRESH_BINARY)
                        val contours: List<MatOfPoint> = ArrayList()
                        val hierarchy = Mat()
                        Imgproc.erode(matCny, matCny, Imgproc.getStructuringElement(
                                        Imgproc.MORPH_RECT,
                                        org.opencv.core.Size(6.0, 6.0)
                                )
                        )
                        Imgproc.dilate(matCny, matCny,
                                Imgproc.getStructuringElement(
                                        Imgproc.MORPH_RECT,
                                        org.opencv.core.Size(12.0, 12.0)
                                )
                        )
                        Imgproc.findContours(matCny, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)
                        Imgproc.drawContours(matBase, contours, -1, Scalar(255.0, 0.0, 0.0), 5)
                        imgBase = Bitmap.createBitmap(matBase.cols(), matBase.rows(), Bitmap.Config.ARGB_8888)
                        Utils.matToBitmap(matBase, imgBase)
                        Thread { runOnUiThread { binding.imageView.setImageBitmap(imgBase) } }.start()
                        imgRoi = Bitmap.createBitmap(matCny.cols(), matCny.rows(), Bitmap.Config.ARGB_8888)
                        Utils.matToBitmap(matCny, imgRoi)
                        var idx = 0
                        while (idx >= 0) {
                            val matOfPoint = contours[idx]
                            val rect = Imgproc.boundingRect(matOfPoint)
                            if (rect.width < 30 || rect.height < 30 || rect.width <= rect.height || rect.width <= rect.height * 3 || rect.width >= rect.height * 6) {
                                idx = hierarchy[0, idx][0].toInt()
                                continue
                            }
                            roi = Bitmap.createBitmap(
                                    imgRoi,
                                    rect.tl().x.toInt(), rect.tl().y.toInt(), rect.width, rect.height
                            )
                            Thread {
                                runOnUiThread {
                                    binding.imageResult.setImageBitmap(roi)
                                    AsyncTess().execute(roi)
                                    binding.takePictureBtn.isEnabled = false
                                }
                            }.start()
                            break
                            idx = hierarchy[0, idx][0].toInt()
                        }
                        //binding.imageView.setImageBitmap(bitmap)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        image?.close()
                    }
                }

            imageReader.setOnImageAvailableListener(readerListener, mBackgroundHandler)
            val captureListener: CameraCaptureSession.CaptureCallback = object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(
                        session: CameraCaptureSession,
                        request: CaptureRequest,
                        result: TotalCaptureResult
                ) {
                    super.onCaptureCompleted(session, request, result)
                    createCameraPreview()
                }
            }
            cameraDevice!!.createCaptureSession(
                    outputSurfaces,
                    object : CameraCaptureSession.StateCallback() {
                        override fun onConfigured(session: CameraCaptureSession) {
                            try {
                                session.capture(
                                        captureBuilder.build(),
                                        captureListener,
                                        mBackgroundHandler
                                )
                            } catch (e: CameraAccessException) {
                                e.printStackTrace()
                            }
                        }

                        override fun onConfigureFailed(session: CameraCaptureSession) {}
                    },
                    mBackgroundHandler
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    var textureListener: TextureView.SurfaceTextureListener = object :
        TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            //open your camera here
            openCamera()
        }

        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
            // Transform you image captured size according to the surface width and height
        }

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
            return false
        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
    }



    private val stateCallback: CameraDevice.StateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            //This is called when the camera is open
            Log.e("rongmera", "onOpened")
            // cameradevice 객체 생성
            cameraDevice = camera
            // 촬영을 위한 준비
            createCameraPreview()
        }

        override fun onDisconnected(camera: CameraDevice) {
            cameraDevice!!.close()
        }

        override fun onError(camera: CameraDevice, error: Int) {
            cameraDevice!!.close()
            cameraDevice = null
        }
    }

    protected fun startBackgroundThread() {
        mBackgroundThread = HandlerThread("Camera Background")
        mBackgroundThread!!.start()
        mBackgroundHandler = Handler(mBackgroundThread!!.looper)
    }

    protected fun stopBackgroundThread() {
        mBackgroundThread!!.quitSafely()
        try {
            mBackgroundThread!!.join()
            mBackgroundThread = null
            mBackgroundHandler = null
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    private fun closeCamera() {
        if (null != imageReader) {
            imageReader!!.close()
            imageReader = null
        }
    }
    // 촬영을 위한 준비
    protected fun createCameraPreview() {
        try {
            val texture = binding.textureView.surfaceTexture!!
            texture.setDefaultBufferSize(imageDimension!!.width, imageDimension!!.height)
            val surface = Surface(texture)

            captureRequestBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            captureRequestBuilder!!.addTarget(surface)


            cameraDevice!!.createCaptureSession(Arrays.asList(surface), object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                    //The camera is already closed
                    if (null == cameraDevice) {
                        return
                    }
                    // When the session is ready, we start displaying the preview.
                    cameraCaptureSessions = cameraCaptureSession
                    updatePreview()
                }

                override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {
                    Toast.makeText(this@CheckNumberAcitivity, "Configuration change", Toast.LENGTH_SHORT).show()
                }
            }, null)

        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    protected fun updatePreview() {
        if (null == cameraDevice) {
            Log.e("rongmera", "updatePreview error, return")
        }
        captureRequestBuilder!!.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
        try {
            cameraCaptureSessions!!.setRepeatingRequest(
                    captureRequestBuilder!!.build(),
                    null,
                    mBackgroundHandler
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    // 카메라 정보를 받아오고 카메라를 open시킨다.
    private fun openCamera() {
        // 카메라 인스턴스 생성
        val manager = getSystemService(CAMERA_SERVICE) as CameraManager
        Log.e("rongmera", "is camera open")
        try {
            //카메라 선택(디폴트)
            cameraId = manager.cameraIdList[0]
            // 선택한 카메라의 특징
            val characteristics = manager.getCameraCharacteristics(cameraId!!)
            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!


            imageDimension = map.getOutputSizes(SurfaceTexture::class.java)[0]


            // 카메라 권한 확인
            if (ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.CAMERA
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                        this@CheckNumberAcitivity,
                        arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        REQUEST_CAMERA_PERMISSION
                )
                return
            }
            manager.openCamera(cameraId!!, stateCallback, null)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
        Log.e("rongmera", "openCamera")
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<String>,
            grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                // close the app
                Toast.makeText(
                        this@CheckNumberAcitivity,
                        "사진촬영 권한을 승인해주세요.",
                        Toast.LENGTH_LONG
                ).show()
                finish()
            }
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



    override fun onResume() {
        super.onResume()
        Log.e("rongmera", "onResume")
        startBackgroundThread()
        if(::binding.isInitialized) {
            if (binding.textureView.isAvailable) {
                openCamera()
            } else {
                binding.textureView.surfaceTextureListener = textureListener
            }
        }
    }



    override fun onPause() {
        Log.e("rongmera", "onPause")
        closeCamera()
        stopBackgroundThread()
        super.onPause()
    }

    private inner class AsyncTess : AsyncTask<Bitmap?, Int?, String>() {

        override fun onPostExecute(result: String) {
            var result = result
            val match = "[^\uAC00-\uD7A3xfe0-9a-zA-Z\\s]"
            result = result.replace(match.toRegex(), " ")
            result = result.replace(" ".toRegex(), "")
            if (result.length in 7..8) {
                binding.textView.text = result
                Toast.makeText(this@CheckNumberAcitivity, "" + result, Toast.LENGTH_SHORT).show()
            } else {
                binding.textView.text = ""
                Toast.makeText(this@CheckNumberAcitivity, "번호판 문자인식에 실패했습니다", Toast.LENGTH_LONG).show()
            }
            binding.takePictureBtn.isEnabled = true
            binding.takePictureBtn.text = "텍스트 인식"
        }

        override fun doInBackground(vararg params: Bitmap?): String {
            tessBaseAPI!!.setImage(params[0])
            return tessBaseAPI!!.utF8Text
        }
    }
}
