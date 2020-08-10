package pl.maciejsadyapps.emotiondetector.ui

import android.graphics.Bitmap
import android.graphics.Rect
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.view.PreviewView
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.face.FirebaseVisionFace
import org.tensorflow.lite.Interpreter
import pl.maciejsadyapps.emotiondetector.domain.CameraManager
import pl.maciejsadyapps.emotiondetector.domain.EmotionAnalyzer
import pl.maciejsadyapps.emotiondetector.extension.createMat
import pl.maciejsadyapps.emotiondetector.extension.crop
import pl.maciejsadyapps.emotiondetector.extension.grayScale
import pl.maciejsadyapps.emotiondetector.extension.normalize
import pl.maciejsadyapps.emotiondetector.extension.resize
import pl.maciejsadyapps.emotiondetector.extension.run
import pl.maciejsadyapps.emotiondetector.extension.to4DArray
import pl.maciejsadyapps.emotiondetector.extension.toBitmap
import pl.maciejsadyapps.emotiondetector.model.Emotion
import pl.maciejsadyapps.emotiondetector.model.Face
import timber.log.Timber

class MainViewModel(
    private val emotionAnalyzer: EmotionAnalyzer,
    private val cameraManager: CameraManager,
    private val emotionInterpreter: Interpreter
) : ViewModel() {

    val faces = MutableLiveData<List<Face>>()

    init {
        emotionAnalyzer.onFacesDetected(::onFaceDetected)
    }

    fun startCamera(activity: AppCompatActivity, cameraView: PreviewView) {
        cameraManager.startCamera(activity, cameraView, emotionAnalyzer)
    }

    private fun onFaceDetected(
        image: FirebaseVisionImage,
        firebaseFaces: List<FirebaseVisionFace>
    ) {
        val processedFaces = firebaseFaces.mapNotNull { face ->
            try {
                val bitmap = image.bitmap
                val faceRect = face.boundingBox
                processFace(bitmap, faceRect)
            } catch (exception: Exception) {
                Timber.e(exception)
                null
            }
        }

        faces.postValue(processedFaces)
    }

    private fun processFace(
        input: Bitmap,
        faceRect: Rect
    ): Face {
        val formattedFace = createMat(input)
            .crop(faceRect)
            .grayScale()
            .resize()

        val normalizedFace = formattedFace
            .normalize()
            .to4DArray()

        val result = emotionInterpreter.run(normalizedFace)

        return Face(
            emotion = Emotion.fromResult(result),
            bitmap = formattedFace.toBitmap(),
            imageRect = Rect(0, 0, input.width, input.height),
            faceRect = faceRect
        )
    }
}
