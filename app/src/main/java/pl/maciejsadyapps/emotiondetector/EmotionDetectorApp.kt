package pl.maciejsadyapps.emotiondetector

import android.app.Application
import com.google.firebase.FirebaseApp

class EmotionDetectorApp : Application() {

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(baseContext)
    }
}
