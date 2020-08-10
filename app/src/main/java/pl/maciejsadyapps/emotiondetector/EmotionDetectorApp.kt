package pl.maciejsadyapps.emotiondetector

import android.app.Application
import com.google.firebase.FirebaseApp
import org.opencv.android.OpenCVLoader
import pl.maciejsadyapps.emotiondetector.domain.ViewModelFactory

class EmotionDetectorApp : Application() {

    val viewModelFactory by lazy {
        ViewModelFactory(this)
    }

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(baseContext)
        OpenCVLoader.initDebug()
    }
}
