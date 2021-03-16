package sg.edu.nyp.erobot.helpers

import android.app.Activity
import org.opencv.core.Mat
import org.opencv.core.MatOfRect
import org.opencv.core.Size
import org.opencv.objdetect.CascadeClassifier

object OpenCVFaceDetector
{
    private val TAG = "FaceDetector"

    private var haar: CascadeClassifier? = null

    // Define the variables in which the face search results will be placed
    // (faceBoundingBoxes - rectangular areas)
    //
    private var faceBoundingBoxes: MatOfRect? = null



    /**
     * Initialize the face detector.
     */
    fun initialize(activity: Activity)
    {
        //Helpers.copyFileFromRawFolderToFilesFolder(activity, sg.edu.nyp.erobot.R.raw.haarcascade_frontal_face, "haarcascade_frontal_face.xml")

        this.faceBoundingBoxes = MatOfRect()

        this.haar = CascadeClassifier()
        this.haar!!.load(activity.filesDir.absolutePath + "/haarcascade_frontal_face.xml")
    }

    /**
     * This function detects faces within a frame using the pre-trained Haar Cascade face
     * detector.
     */
    fun detectFacesInFrame(imageGray: Mat, faceBoundingBoxes: MatOfRect)
    {
        haar!!.detectMultiScale(imageGray, faceBoundingBoxes, 1.1, 3, 0,
            Size(50.0, 50.0), Size (300.0, 300.0))
    }
}
