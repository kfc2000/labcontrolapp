package sg.edu.nyp.erobot.helpers

import android.app.Activity
import org.opencv.core.Mat
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import sg.edu.nyp.erobot.ml.MaskDetector2
import java.io.File


object FaceMaskDetector
{
    var maskdetect: MaskDetector2? = null

    /**
     * Initializes our face mask/no-mask classifier.
     */
    fun initialize(activity: Activity)
    {
        maskdetect = MaskDetector2.newInstance(activity)
    }


    /**
     * Detect whether the face has a mask on it.
     *
     * Returns the probabilty that a face has mask
     */
    fun detect(image: Mat) : Float
    {
        var resizedImage = Helpers.resizeImage(image, 224, 224)
        var array = Helpers.matToRGBFloatArray(resizedImage, 1.0/128, -1.0)
        // Creates inputs for reference.
        val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 224, 224, 3), DataType.FLOAT32)
        inputFeature0.loadArray(array)

        // Runs model inference and gets result.
        val outputs = maskdetect!!.process(inputFeature0)
        val outputFeature0 = outputs.outputFeature0AsTensorBuffer   // class 1 / class 2 probabilities

        return outputFeature0.floatArray[0]

    }


}