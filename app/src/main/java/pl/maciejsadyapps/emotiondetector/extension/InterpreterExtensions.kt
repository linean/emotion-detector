package pl.maciejsadyapps.emotiondetector.extension

import org.tensorflow.lite.Interpreter

fun Interpreter.run(input: Any): Array<FloatArray> {
    val output = arrayOf(FloatArray(7))
    run(input, output)
    return output
}
