package sg.edu.nyp.erobot.helpers

import android.app.Activity
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import org.opencv.core.Mat


object MLKitFaceDetector
{
    var faceDetector: FaceDetector? = null

    /**
     * Initialize our face detector
     */
    fun initialize()
    {
        // Real-time contour detection of multiple faces

        // Real-time contour detection of multiple faces
        val options: FaceDetectorOptions = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setContourMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .build()

        faceDetector = FaceDetection.getClient(options)
    }


    /**
     * Detects all faces in the image asynchronously. The onDetected lambda
     * will be called once complete.
     */
    fun detectAsync(image: Mat, onDetected: (List<Face>) -> Unit)
    {
        var bitmap = Helpers.matToBitmap(image)
        val image = InputImage.fromBitmap(bitmap!!, 0)
        faceDetector!!.process(image).addOnSuccessListener {
            result ->
            onDetected(result)
        }
        .addOnFailureListener {
            exc ->
        }

    }
}