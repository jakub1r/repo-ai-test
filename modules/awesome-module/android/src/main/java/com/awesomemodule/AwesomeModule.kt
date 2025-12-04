package com.awesomemodule

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.WritableNativeArray
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.module.annotations.ReactModule
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

@ReactModule(name = AwesomeModule.NAME)
class AwesomeModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    private val options = Interpreter.Options().apply {
        setUseXNNPACK(false)
        setNumThreads(4)
    }

    private val interpreter = Interpreter(loadModelFromRaw(), options)


    override fun getName(): String = NAME

    private fun loadModelFromRaw(): ByteBuffer {
    val resId = reactApplicationContext.resources.getIdentifier(
        "model",       // nazwa pliku bez rozszerzenia
        "raw",
        reactApplicationContext.packageName
    )

    val inputStream = reactApplicationContext.resources.openRawResource(resId)
    val fileBytes = inputStream.readBytes()

    // Konwersja do ByteBuffer
    val byteBuffer = ByteBuffer.allocateDirect(fileBytes.size)
    byteBuffer.order(ByteOrder.nativeOrder())
    byteBuffer.put(fileBytes)
    byteBuffer.rewind()

    return byteBuffer
}

    private fun preprocessImage(bitmap: Bitmap): ByteBuffer {
        val size = 640
        val buffer = ByteBuffer.allocateDirect(4 * 1 * size * size * 3).order(ByteOrder.nativeOrder())
        val resized = Bitmap.createScaledBitmap(bitmap, size, size, true)
        val pixels = IntArray(size * size)
        resized.getPixels(pixels, 0, size, 0, 0, size, size)

        for (v in pixels) {
            buffer.putFloat(((v shr 16) and 0xFF) / 255f)
            buffer.putFloat(((v shr 8) and 0xFF) / 255f)
            buffer.putFloat((v and 0xFF) / 255f)
        }
        return buffer
    }

    @ReactMethod
    fun processImage(path: String, promise: Promise) {
        try {
            val bitmap = BitmapFactory.decodeFile(path)
            val input = preprocessImage(bitmap)
            val output = Array(1) { Array(300) { FloatArray(6) } }
            interpreter.run(input, output)

            val result = WritableNativeArray()
            for (i in 0 until 300) {
                if (output[0][i][4] > 0.3) {
                    val row = WritableNativeArray()
                    output[0][i].forEach { row.pushDouble(it.toDouble()) }
                    result.pushArray(row)
                }
            }
            promise.resolve(result)
        } catch (e: Exception) {
            promise.reject("ERR_PROCESS", e)
        }
    }

    companion object {
        const val NAME = "AwesomeModule"
    }
}
