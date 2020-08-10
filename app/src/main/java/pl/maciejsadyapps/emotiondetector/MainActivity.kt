package pl.maciejsadyapps.emotiondetector

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Rect
import android.graphics.RectF
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.content.ContextCompat
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata.ROTATION_0
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata.ROTATION_180
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata.ROTATION_270
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata.ROTATION_90
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private val cameraExecutor by lazy {
        Executors.newSingleThreadExecutor()
    }

    private val detector by lazy {
        val realTimeOpts = FirebaseVisionFaceDetectorOptions.Builder()
            .setContourMode(FirebaseVisionFaceDetectorOptions.ALL_CONTOURS)
            .build()

        FirebaseVision
            .getInstance()
            .getVisionFaceDetector(realTimeOpts)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions(
                this,
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                toast("Missing camera permission.")
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(
            Runnable {
                val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build()
                preview.setSurfaceProvider(cameraView.createSurfaceProvider())

                val imageAnalyzer = ImageAnalysis.Builder().build()
                imageAnalyzer.setAnalyzer(cameraExecutor, TestAnalyzer())

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        this,
                        CameraSelector.DEFAULT_FRONT_CAMERA,
                        preview,
                        imageAnalyzer
                    )
                } catch (exc: Exception) {
                    toast("Unable to connect to the camera.")
                }
            },
            ContextCompat.getMainExecutor(this)
        )
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private inner class TestAnalyzer : ImageAnalysis.Analyzer {

        private fun degreesToFirebaseRotation(degrees: Int): Int = when (degrees) {
            0 -> ROTATION_0
            90 -> ROTATION_90
            180 -> ROTATION_180
            270 -> ROTATION_270
            else -> throw Exception("Rotation must be 0, 90, 180, or 270.")
        }

        @SuppressLint("UnsafeExperimentalUsageError")
        override fun analyze(image: ImageProxy) {
            image.image
                ?.let { mediaImage ->
                    FirebaseVisionImage.fromMediaImage(
                        mediaImage,
                        degreesToFirebaseRotation(image.imageInfo.rotationDegrees)
                    )
                }
                ?.let { firebaseImage ->
                    detector.detectInImage(firebaseImage)
                        .addOnSuccessListener { faces ->
                            emotionView.faceRect = faces
                                .firstOrNull()
                                ?.let { previewRect(image, it.boundingBox) }
                        }
                        .addOnCompleteListener { image.close() }
                }
        }
    }

    // Works only for 270 degree
    private fun previewRect(image: ImageProxy, faceRect: Rect): RectF {
        val scaleY = emotionView.height / image.width.toFloat()
        val scaleX = emotionView.width / image.height.toFloat()

        return RectF(
            emotionView.width - faceRect.left * scaleX,
            faceRect.top * scaleY,
            emotionView.width - faceRect.right * scaleX,
            faceRect.bottom * scaleY
        )
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}
