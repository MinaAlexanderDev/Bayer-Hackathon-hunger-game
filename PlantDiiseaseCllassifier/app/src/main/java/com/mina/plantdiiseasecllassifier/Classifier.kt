package com.mina.plantdiiseasecllassifier

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.*

class Classifier(assetManager: AssetManager, modelPath: String, labelPath: String, inputSize: Int) {
    private var INTERPRETER: Interpreter
    private var LABEL_LIST: List<String>
    private val INPUT_SIZE: Int = inputSize
    private val PIXEL_SIZE: Int = 3
    private val IMAGE_MEAN = 0
    private val IMAGE_STD = 255.0f
    private val MAX_RESULTS = 3
    private val THRESHOLD = 0.4f
    private val TAG = "Classifier"

    data class Recognition(
        var id: String = "",
        var title: String = "",
        var confidence: Float = 0F
    ) {
        override fun toString(): String {
            return "Title = $title, Confidence = $confidence)"
        }
    }

    init {
        // Lunch TF INTERPRETER
        INTERPRETER = Interpreter(loadModelFile(assetManager, modelPath))
        // Load list of classes
        LABEL_LIST = loadLabelList(assetManager, labelPath)

        Log.d(TAG, "LABEL_LIST11 : " + LABEL_LIST)
    }
    //load file from assets resources
    private fun loadModelFile(assetManager: AssetManager, modelPath: String): MappedByteBuffer {
        val fileDescriptor = assetManager.openFd(modelPath)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun loadLabelList(assetManager: AssetManager, labelPath: String): List<String> {
        return assetManager.open(labelPath).bufferedReader().useLines { it.toList() }

    }

    fun recognizeImage(bitmap: Bitmap): List<Classifier.Recognition> {
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, false)
        val byteBuffer = convertBitmapToByteBuffer(scaledBitmap)


        val result = Array(1) { FloatArray(LABEL_LIST.size) }
        Log.d(TAG, "result : " + result)
        INTERPRETER.run(byteBuffer, result)
        Log.d("Classifier", "byteBuffer  : " + byteBuffer)
        Log.d("Classifier", "result 2 : " + result)
        return getSortedResult(result)
    }


    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(4 * INPUT_SIZE * INPUT_SIZE * PIXEL_SIZE)
        byteBuffer.order(ByteOrder.nativeOrder())
        val intValues = IntArray(INPUT_SIZE * INPUT_SIZE)

        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        var pixel = 0
        for (i in 0 until INPUT_SIZE) {
            for (j in 0 until INPUT_SIZE) {
                val `val` = intValues[pixel++]

                byteBuffer.putFloat((((`val`.shr(16) and 0xFF) - IMAGE_MEAN) / IMAGE_STD))
                byteBuffer.putFloat((((`val`.shr(8) and 0xFF) - IMAGE_MEAN) / IMAGE_STD))
                byteBuffer.putFloat((((`val` and 0xFF) - IMAGE_MEAN) / IMAGE_STD))
            }
        }
        return byteBuffer
    }


    private fun getSortedResult(labelProbArray: Array<FloatArray>): List<Classifier.Recognition> {
        Log.d(
            TAG,
            "List Size:(%d, %d, %d)".format(
                labelProbArray.size,
                labelProbArray[0].size,
                LABEL_LIST.size
            )
        )
        Log.d(TAG, "labelProbArray1" + labelProbArray)


        val pq = PriorityQueue(
            MAX_RESULTS,
            Comparator<Classifier.Recognition> { (_, _, confidence1), (_, _, confidence2)
                ->
                java.lang.Float.compare(confidence1, confidence2) * -1
            })

        for (i in LABEL_LIST.indices) {
            Log.d(TAG, " LABEL_LIST.indices : " + LABEL_LIST.indices + "-- i: " + i)
//            Log.d("Classifier", "confidence : "+labelProbArray[0][i] )
            val confidence = labelProbArray[0][i]
            Log.d(TAG, "confidence 1: " + confidence)
            Log.d(
                TAG,
                (" confidence >= THRESHOLD : " + confidence >= THRESHOLD.toString()).toString()
            )

            Log.d(TAG, (" confidence : " + confidence + "-- THRESHOLD : " + THRESHOLD).toString())
            if (confidence >= THRESHOLD) {
                Log.d("Classifier", "confidence 1 : " + confidence)

                pq.add(
                    Classifier.Recognition(
                        "" + i,
                        if (LABEL_LIST.size > i) LABEL_LIST[i] else "Unknown", confidence
                    )

                )
                Log.d(
                    TAG,
                    ("LABEL_LIST.size > i : " + (LABEL_LIST.size > i).toString() + "--  LABEL_LIST[i]  : " + (LABEL_LIST[i])).toString()
                )
                Log.d(TAG, "pq 2 : " + pq.size + "   ---  pq : " + pq)
                Log.d(TAG, " i 2: " + i)
            }
        }
        Log.d(TAG, "pq 3: " + pq)
        Log.d(TAG, "pqsize:(%d)".format(pq.size))

        val recognitions = ArrayList<Classifier.Recognition>()
        Log.d(TAG, "recognitions : " + recognitions)
        Log.d(TAG, "pq.size : " + pq.size + "  --- MAX_RESULTS : " + MAX_RESULTS)
        val recognitionsSize = Math.min(pq.size, MAX_RESULTS)
        Log.d(TAG, "recognitionsSize : " + recognitionsSize)
        for (i in 0 until recognitionsSize) {

            Log.d(TAG, "recognitionsSize 2 : " + recognitionsSize)
            recognitions.add(pq.poll())

        }
        Log.d(TAG, "pqsize:(%d)".format(pq.size) + "-- recognitions : " + recognitions)
        return recognitions
    }

}