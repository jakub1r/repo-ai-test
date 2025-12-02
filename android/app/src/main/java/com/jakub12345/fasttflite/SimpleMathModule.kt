package com.jakub12345.fasttflite

import android.graphics.BitmapFactory
import android.graphics.Bitmap
import com.facebook.react.bridge.*
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.nio.ByteBuffer
import java.nio.ByteOrder

class SimpleMathModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {
    val options = Interpreter.Options().apply {
    setUseXNNPACK(false)   // konieczne dla YOLOv8 TFLite
    setNumThreads(4)
}
val interpreter = Interpreter(loadModelFile("model.tflite"), options)

    override fun getName(): String {
        return "SimpleMath"
    }

    private fun loadModelFile(modelName: String): MappedByteBuffer {
        val fileDescriptor = reactApplicationContext.assets.openFd(modelName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        return fileChannel.map(
            FileChannel.MapMode.READ_ONLY,
            fileDescriptor.startOffset,
            fileDescriptor.declaredLength
        )
    }

    /** Convert image → float32 tensor [1,640,640,3] */
    private fun processBitmap(path: String): FloatArray {
        val bitmap = BitmapFactory.decodeFile(path)
        val resized = Bitmap.createScaledBitmap(bitmap, 640, 640, true)

        val input = FloatArray(1 * 640 * 640 * 3)
        var index = 0

        for (y in 0 until 640) {
            for (x in 0 until 640) {
                val pixel = resized.getPixel(x, y)

                input[index++] = ((pixel shr 16) and 0xFF) / 255f  // R
                input[index++] = ((pixel shr 8) and 0xFF) / 255f   // G
                input[index++] = (pixel and 0xFF) / 255f           // B
            }
        }

        return input
    }

    private fun preprocessImage(bitmap: Bitmap): ByteBuffer {
    val inputSize = 640
    val imgData = ByteBuffer.allocateDirect(4 * 1 * inputSize * inputSize * 3)
    imgData.order(ByteOrder.nativeOrder())

    val resized = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)

    val intValues = IntArray(inputSize * inputSize)
    resized.getPixels(intValues, 0, inputSize, 0, 0, inputSize, inputSize)

    var pixel = 0
    for (i in 0 until inputSize) {
        for (j in 0 until inputSize) {
            val v = intValues[pixel++]

            val r = ((v shr 16) and 0xFF) / 255f
            val g = ((v shr 8) and 0xFF) / 255f
            val b = (v and 0xFF) / 255f

            imgData.putFloat(r)
            imgData.putFloat(g)
            imgData.putFloat(b)
        }
    }

    return imgData
}

    @ReactMethod
    fun processImage(path: String, promise: Promise) {
        try {
            val bitmap = BitmapFactory.decodeFile(path)
            val input = preprocessImage(bitmap)

// Output dla YOLOv8: [1,300,6]
val output = Array(1) { Array(300) { FloatArray(6) } }

interpreter.run(input, output)

            // Convert to JS-friendly structure
            val resultArray = WritableNativeArray()

           for (i in 0 until 300) {
    // sprawdzamy warunek na 5. elemencie (index 4), conf > 0.3
    if (output[0][i][4] > 0.3) {
        val row = WritableNativeArray()
        for (j in 0 until 6) {
            row.pushDouble(output[0][i][j].toDouble())
        }
        resultArray.pushArray(row)
    }
}
            // conf > 0.3
            promise.resolve(resultArray)

        } catch (e: Exception) {
            promise.reject("ERR_PROCESS", e)
        }
    }
}
