package com.kermit.cameraforkotlin

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val TAG:String = "MainActivity"
    private val TAG_KERMIT:String = "Kermit "

    private var cameraManager: CameraManager? = null
    private var mCameraDevice: CameraDevice? = null
    private var mainHandler: Handler? = null
    private var childHandler: Handler? = null
    private var mCaptureRequestBuilder: CaptureRequest.Builder? = null
    private var mImageReader: ImageReader? = null
    private var mCameraCaptureSession: CameraCaptureSession? = null

    private var mSurfaceHolder: SurfaceHolder? = null
    private var previewSurface: Surface? = null

    companion object {
        private const val MY_PERMISSIONS_REQUEST_READ_CONTACTS = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.d(TAG, TAG_KERMIT + "onCreate")
        mSurfaceHolder = surfaceView.holder
        mSurfaceHolder?.addCallback(mSurfaceHolderCallback)
        previewSurface = mSurfaceHolder?.surface
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, TAG_KERMIT + "onStart")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, TAG_KERMIT + "onResume")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, TAG_KERMIT + "onPause")
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, TAG_KERMIT + "onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, TAG_KERMIT + "onDestroy")
    }

    override fun onRequestPermissionsResult(requestCode: Int,  permissions: Array<String>, grantResults: IntArray) {
        Log.d(TAG, TAG_KERMIT + "requestCode $requestCode")
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_READ_CONTACTS -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                } else {
                    finish()
                }
                return
            }
        }
    }

    private fun checkCameraPermission(){
        if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            Log.d(TAG, TAG_KERMIT + "checkCameraPermission hasn't permission")
            if (ActivityCompat.shouldShowRequestPermissionRationale(this@MainActivity, Manifest.permission.CAMERA)) {
                Log.d(TAG, TAG_KERMIT + "shouldShowRequestPermissionRationale true")
                AlertDialog.Builder(this@MainActivity)
                    .setMessage("Please give me some permission")
                    .setPositiveButton("OK") { _, _ ->
                        ActivityCompat.requestPermissions(this@MainActivity,
                            arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE),
                            MY_PERMISSIONS_REQUEST_READ_CONTACTS)
                    }
                    .setNegativeButton("No") { _, _ -> finish() }
                    .show()
            } else {
                Log.d(TAG, TAG_KERMIT + "shouldShowRequestPermissionRationale false")
                ActivityCompat.requestPermissions(this@MainActivity,
                    arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE),
                    MY_PERMISSIONS_REQUEST_READ_CONTACTS)
            }
        }
        else{
            Log.d(TAG, TAG_KERMIT + "checkCameraPermission has permission")
        }

    }

    private fun startCameraSession() {
        Log.d(TAG, TAG_KERMIT + "startCameraSession")

        var mHandlerThread = HandlerThread("Camera2")
        mHandlerThread.start()
        childHandler = Handler(mHandlerThread.looper)
        mainHandler = Handler(mainLooper)

        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        if (cameraManager!!.cameraIdList.isEmpty()) {
            Log.d(TAG, "Kermit no camera")
            return
        }

        Log.d(TAG, TAG_KERMIT + "Cameara Value ${cameraManager?.cameraIdList?.size}")
        val firstCamera = cameraManager!!.cameraIdList[0]
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            cameraManager!!.openCamera(firstCamera, mCameraDeviceStateCallback, mainHandler)
        }
    }

    private fun createCameraPreviewSessionLocked() {
        Log.d(TAG, TAG_KERMIT + "createCameraPreviewSessionLocked")
        mCaptureRequestBuilder = mCameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        previewSurface?.let { mCaptureRequestBuilder?.addTarget(it) }
        mCameraDevice!!.createCaptureSession(arrayListOf(previewSurface, mImageReader!!.surface), mStateCallbackSession, childHandler)
    }

    private val mSurfaceHolderCallback = object: SurfaceHolder.Callback{
        override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
            Log.d(TAG, TAG_KERMIT + "surfaceChanged")
            checkCameraPermission()
            startCameraSession()
        }

        override fun surfaceDestroyed(holder: SurfaceHolder?) {
            Log.d(TAG, TAG_KERMIT + "surfaceDestroyed")
        }

        override fun surfaceCreated(holder: SurfaceHolder?) {
            Log.d(TAG, TAG_KERMIT + "surfaceCreated")
        }
    }

    private val mCameraDeviceStateCallback = object: CameraDevice.StateCallback(){
        
        override fun onOpened(camera: CameraDevice) {
            Log.d(TAG, TAG_KERMIT + "onOpened")
            mCameraDevice = camera
            val cameraCharacteristics = cameraManager!!.getCameraCharacteristics(camera.id)
            cameraCharacteristics[CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP]?.let {
                    streamConfigurationMap -> streamConfigurationMap.getOutputSizes(ImageFormat.YUV_420_888)?.let {
                        yuvSizes -> val previewSize = yuvSizes.last()
                        mImageReader = ImageReader.newInstance(yuvSizes[0].width, yuvSizes[0].height, ImageFormat.JPEG, 5)
                        mImageReader?.setOnImageAvailableListener(mImageReaderOnImageAvailableListener, mainHandler)
                        Log.d(TAG, TAG_KERMIT + "Kermit W:" + yuvSizes[0].width + " H:" + yuvSizes[0].height)
                    }
            }
            createCameraPreviewSessionLocked()
        }

        override fun onDisconnected(camera: CameraDevice) {
            Log.d(TAG, TAG_KERMIT + "onDisconnected")
            camera?.close()
            mCameraDevice = null
        }

        override fun onError(camera: CameraDevice, error: Int) {
            Log.d(TAG, TAG_KERMIT + "onError")
            camera?.close()
            mCameraDevice = null
        }

    }

    private val mStateCallbackSession = object: CameraCaptureSession.StateCallback(){
        override fun onConfigureFailed(session: CameraCaptureSession) {
            Log.d(TAG, TAG_KERMIT + "onConfigureFailed")
        }

        override fun onConfigured(session: CameraCaptureSession) {
            Log.d(TAG, TAG_KERMIT + "onConfigured")
            mCameraCaptureSession = session
            mCaptureRequestBuilder?.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
            mCaptureRequestBuilder?.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            var mCaptureRequest = mCaptureRequestBuilder?.build();
            mCaptureRequest?.let { mCameraCaptureSession?.setRepeatingRequest(it, mCameraCaptureSessionCaptureCallback, childHandler) }
        }
    }

    private val mImageReaderOnImageAvailableListener =
        ImageReader.OnImageAvailableListener { reader ->
            Log.d(TAG, TAG_KERMIT + "onImageAvailable")
            var mImage = reader?.acquireLatestImage()
            var buffer = mImage?.planes?.get(0)?.buffer
            Log.d(TAG, TAG_KERMIT + "Length " + buffer?.remaining())
        }

    private val mCameraCaptureSessionCaptureCallback = object: CameraCaptureSession.CaptureCallback() {

        override fun onCaptureStarted(session: CameraCaptureSession, request: CaptureRequest, timestamp: Long, frameNumber: Long) {
            //Log.d(TAG, TAG_KERMIT + "onCaptureStarted");
            request?.tag
        }

        override fun onCaptureBufferLost(session: CameraCaptureSession, request: CaptureRequest, target: Surface, frameNumber: Long) {
            //Log.d(TAG, TAG_KERMIT + "onCaptureBufferLost");
        }

        override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult) {
            //Log.d(TAG, TAG_KERMIT + "onCaptureCompleted");
        }

        override fun onCaptureFailed(session: CameraCaptureSession, request: CaptureRequest, failure: CaptureFailure) {
            //Log.d(TAG, TAG_KERMIT + "onCaptureFailed");
        }

        override fun onCaptureProgressed(session: CameraCaptureSession, request: CaptureRequest, partialResult: CaptureResult) {
            //Log.d(TAG, TAG_KERMIT + "onCaptureProgressed");
        }

        override fun onCaptureSequenceAborted(session: CameraCaptureSession, sequenceId: Int) {
            //Log.d(TAG, TAG_KERMIT + "onCaptureSequenceAborted");
        }

        override fun onCaptureSequenceCompleted(session: CameraCaptureSession, sequenceId: Int, frameNumber: Long) {
            //Log.d(TAG, TAG_KERMIT + "onCaptureSequenceCompleted");
        }
    }

}