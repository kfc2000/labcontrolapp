package sg.edu.nyp.erobot.helpers

import android.util.Log
import org.opencv.core.*
import org.opencv.objdetect.HOGDescriptor

object OpenCVPeopleDetector
{
    private val TAG = "PeopleDetector"

    // Our HoG descriptors which will be used to detect people
    //
    private var hog: HOGDescriptor? = null


    /**
     * Initialize the people detector.
     */
    fun initialize()
    {
        Log.i(TAG, "Initializing the HOG people detector.")

        hog = HOGDescriptor(Size(64.0, 128.0), Size(16.0, 16.0), Size(8.0,8.0), Size(8.0, 8.0), 9) // construct the HOG descriptor
        hog!!.setSVMDetector(HOGDescriptor.getDefaultPeopleDetector())
    }




    /**
     * This function detects people within a frame using the pre-trained HoG people
     * detector.
     */
    fun detectPeopleInFrame(imageGray: Mat, peopleBoundingBoxes: MatOfRect, peopleWeights: MatOfDouble)
    {
        //The analysis of the photos itself. Results will be recorded in locations and weights
        hog!!.detectMultiScale(imageGray, peopleBoundingBoxes, peopleWeights, 0.0,
            Size(8.0, 8.0), Size(8.0, 8.0), 1.1, 2.0, false)
    }

}