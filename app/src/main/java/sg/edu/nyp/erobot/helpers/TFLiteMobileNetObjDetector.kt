package sg.edu.nyp.erobot.helpers


import android.app.Activity
import org.opencv.core.Mat
import sg.edu.nyp.erobot.R
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.vision.detector.Detection
import org.tensorflow.lite.task.vision.detector.ObjectDetector
import org.tensorflow.lite.task.vision.detector.ObjectDetector.ObjectDetectorOptions
import java.io.File


object TFLiteMobileNetObjDetector
{
    var activity: Activity? = null

    var objectDetector: ObjectDetector? = null

    fun initialize(activity: Activity)
    {
        this.activity = activity

        Helpers.copyFileFromRawFolderToFilesFolder(activity, R.raw.ssd_mobilenet, "ssd_mobilenet.tflite")

        // Initialization
        var options = ObjectDetectorOptions.builder().setMaxResults(1).build()


        val modelFile = File(activity.filesDir.absolutePath + "/ssd_mobilenet.tflite")

        if (modelFile.exists()) {
            this.objectDetector = ObjectDetector.createFromFileAndOptions(modelFile, options)
        }
    }

    fun detect(image: Mat) : List<Detection>
    {
        val bitmap = Helpers.matToBitmap(image)
        val image = TensorImage.fromBitmap(bitmap)
        return this.objectDetector!!.detect(image)
    }
}