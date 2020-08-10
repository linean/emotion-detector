package pl.maciejsadyapps.emotiondetector.extension

import android.graphics.Rect

fun Rect.toOpenCVRect(): org.opencv.core.Rect {
    return org.opencv.core.Rect(left, top, width(), height())
}
