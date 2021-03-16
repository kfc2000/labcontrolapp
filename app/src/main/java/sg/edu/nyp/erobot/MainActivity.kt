package sg.edu.nyp.erobot

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.SurfaceView
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.mlkit.vision.face.Face

import org.opencv.android.*
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import sg.edu.nyp.erobot.helpers.*
import sg.edu.nyp.erobot.robothelpers.RobotInterface
import kotlin.concurrent.fixedRateTimer

import androidx.databinding.DataBindingUtil
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
//import sg.edu.nyp.erobot.databinding.ActivityMainBinding
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.ktx.storage
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONException
import org.json.JSONObject
import sg.edu.nyp.erobot.helpers.Helpers.createTemoraryFile
import sg.edu.nyp.erobot.helpers.Helpers.saveImage
import java.io.File
import java.io.FileInputStream
import java.util.*
import kotlin.collections.HashMap

class MainActivity : AppCompatActivity(), CameraBridgeViewBase.CvCameraViewListener2 {

    companion object {
        //        val ACTION_HOME_WELCOME = "home.welcome"
        //        val ACTION_HOME_DANCE = "home.dance"
        //        val ACTION_HOME_SLEEP = "home.sleep"
        //        val HOME_BASE_LOCATION = "home base"
        //
        //        // Storage Permissions
        private val REQUEST_EXTERNAL_STORAGE = 1
        private val REQUEST_CODE_NORMAL = 0
        private val REQUEST_CODE_FACE_START = 1
        private val REQUEST_CODE_FACE_STOP = 2
        private val REQUEST_CODE_MAP = 3
        private val REQUEST_CODE_SEQUENCE_FETCH_ALL = 4
        private val REQUEST_CODE_SEQUENCE_PLAY = 5
        private val REQUEST_CODE_START_DETECTION_WITH_DISTANCE = 6
        private val PERMISSIONS_STORAGE = arrayOf<String>(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        private val neededPermissions =
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)

        private const val TAG = "MainActivity"
        private const val CAMERA_PERMISSION_REQUEST = 1
        const val REQUEST_CODE = 100
    }

    //Firebase Cloud Messaging
    private val FCM_API = "https://fcm.googleapis.com/fcm/send"
    private val serverKey =
            "key=" + "AAAAo5DWCHk:APA91bE_AsdGvJrgwSFn7ZdVoojdcpvUodxdSS2NQiOqbx5FVUF9vZheqM5UsTqhg_ZkW1UwG3kFsxoKnT2P80-MgL09CDF1idiszUA3YtpNziRqIJ1gdB_fgXiWxygskr_93jcd4WFB"
    private val contentType = "application/json"
    private val requestQueue: RequestQueue by lazy {
        Volley.newRequestQueue(this.applicationContext)
    }

    // var temiRobot : Robot? = null
    var currentLocationIndex: Int = 0

    var isPatrolling: Boolean = false

    var newtiming: org.joda.time.LocalTime = org.joda.time.LocalTime.now()
    var prevtiming: org.joda.time.LocalTime = org.joda.time.LocalTime.now()
    var timings: Array<org.joda.time.LocalTime> = arrayOf(
            org.joda.time.LocalTime(2, 0),
            org.joda.time.LocalTime(3, 0),
            org.joda.time.LocalTime(4, 0),
            org.joda.time.LocalTime(5, 0),
            org.joda.time.LocalTime(6, 0),
            org.joda.time.LocalTime(7, 0)
    )

    // The camera surface view
    //
    private var mOpenCvCameraView: JavaCameraView? = null

    // Some global image matrixes to hold contents of the image from the video
    // frames.
    //
    private var imageRgba: Mat? = null

    // Define the variables in which the people search results will be placed
    // (peopleBoundingBoxes - rectangular areas, peopleWeights - weight
    // (we can say relevance) of the corresponding location)
    //
    private var peopleBoundingBoxes: MatOfRect? = null
    private var peopleWeights: MatOfDouble? = null

    // Define the variables in which the face search results will be placed
    // (faceBoundingBoxes - rectangular areas)
    //
    private var faceBoundingBoxes: MatOfRect? = null
    private var faceDetections: List<Face>? = null

    // Some variables for drawing the bounding boxes of the face / people
    //
    private var peopleRectColor: Scalar? = null
    private var faceNoMaskRectColor: Scalar? = null
    private var faceMaskRectColor: Scalar? = null
    private var objRectColor: Scalar? = null

    // Flag to indicated that frame is still currently being analyzed.
    // (this is used to prevent re-entry every frame)
    //
    private var isAnalyzingFrame = false
    private var detectedPeopleFrameCounter = 0
    private var faceWithNoMaskFrameCounter = 0

    private var robot: RobotInterface? = null

    /**
     * This callback contains an onManagerConnected function that is triggered
     * once OpenCV is loaded / failed to load.
     */
    private val mLoaderCallback = object : BaseLoaderCallback(this) {
        override fun onManagerConnected(status: Int) {
            when (status) {
                LoaderCallbackInterface.SUCCESS -> {
                    Log.i(TAG, "OpenCV loaded successfully")

                    // Initializes the HOG people detector.
                    //
                    //OpenCVPeopleDetector.initialize()
                    //OpenCVFaceDetector.initialize(this@MainActivity)
                    TFLiteMobileNetObjDetector.initialize(this@MainActivity)
                    FaceMaskDetector.initialize(this@MainActivity)
                    MLKitFaceDetector.initialize()

                    peopleBoundingBoxes = MatOfRect()
                    peopleWeights = MatOfDouble()
                    faceBoundingBoxes = MatOfRect()

                    peopleRectColor = Scalar(255.0, 255.0, 0.0)
                    faceMaskRectColor = Scalar(0.0, 255.0, 0.0)
                    faceNoMaskRectColor = Scalar(255.0, 0.0, 0.0)
                    objRectColor = Scalar(255.0, 255.0, 255.0)

                    // Enable the camera view
                    //
                    if (mOpenCvCameraView != null) {
                        mOpenCvCameraView!!.enableView()
                    }
                }
                else -> {
                    super.onManagerConnected(status)
                }
            }
        }
    }

    /**
     * Called when the activity is created.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(TAG, "called onCreate")
        super.onCreate(savedInstanceState)

        // Load the layout for this activity.
        //
        setContentView(R.layout.activity_main)

        // Initialize OpenCV.
        //
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization")
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback)
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!")
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        }

        // Grab the camera surface and initialize it.
        //
        mOpenCvCameraView = findViewById<JavaCameraView>(R.id.surfaceView)
        mOpenCvCameraView!!.enableView()
        mOpenCvCameraView!!.visibility = SurfaceView.VISIBLE
        mOpenCvCameraView!!.setMaxFrameSize(1024, 768)

        mOpenCvCameraView!!.setCvCameraViewListener(this)

        Log.d(TAG, "Camera requesting permissions...")
        // Request for permissions to access the camera.
        // Permissions for Android 6+
        //
        ActivityCompat.requestPermissions(
                this@MainActivity,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST
        )


        val btnPatrol: Button = findViewById(R.id.btnPatrol)
        btnPatrol.setOnClickListener {
            startPatrol()
        }

        val btnQuit: Button = findViewById(R.id.btnQuit)
        btnQuit.setOnClickListener {
            finishAffinity();
            System.exit(0);
        }

        val btnReturnToZero: Button = findViewById(R.id.btnReturnToZero)
        btnReturnToZero.setOnClickListener {
            robot!!.goToPosition(0.0f, 0.0f, 0.0f)
        }

        val btnSendNotification: Button = findViewById(R.id.btnSendNotification)
        btnSendNotification.setOnClickListener {
            val topic = "/topics/Enter_topic" //topic has to match what the receiver subscribed to

            val notification = JSONObject()
            val notifcationBody = JSONObject()

            try {
                notifcationBody.put("title", "SDA Tracker App")
                notifcationBody.put("message", "Suspicious Activity Detected Here!")
                notification.put("to", topic)
                notification.put("data", notifcationBody)
                Log.e("TAG", "try")
            } catch (e: JSONException) {
                Log.e("TAG", "onCreate: " + e.message)
            }

            sendNotification(notification)
        }

        // Initialize the robot interface
        // (When running on Temi, ensure that you choose Build > Select Build Variant > debugTemi)
        // (When running on an emulator, all these functions are just empty stubs)
        //
        robot = RobotInterface()
        robot!!.setActivity(this)
        robot!!.initializeRobot {
            robot!!.speak("Starting...")
            robot!!.goToLocationByName("HOME BASE") {
                robot!!.speak("I am home and ready!")
            }
        }

        // This fixed rate timer runs the section of code inside every 1 second
        // for handling recurring background tasks
        //
        fixedRateTimer("5s time check", true, 0, 1000) {

            //Log.d(TAG, "Battery: " + robot!!.getBatteryLevel())

            // Retrieve the position every 1 second and updating
            // it in the user interface for debugging purposes.
            //
            var textView: TextView = findViewById(R.id.textView)
            val position = robot!!.getPosition()
            runOnUiThread {
                //Log.d(TAG, "Position: " + position.x + "," + position.y + "," + position.rotateAngle)
                textView.text = "Position: " + position.x + "," + position.y + "," + position.rotateAngle
            }

            // Find out if it's time for the next round of patrol.
            // If so, start the next round of patrol
            prevtiming = newtiming
            newtiming = org.joda.time.LocalTime.now()
            for (i in timings) {
                if (i.isAfter(prevtiming) and (i.isBefore(newtiming) or i.isEqual(newtiming))) {
                    startPatrol()
                    break
                }
            }

            //println("========================")
        }
    }


    /**
     * Begins the patrol based on the locations saved within Temi.
     *
     * NOTE: The locations saved in Temi in the reverse order that they
     * were recorded, so we will also patrol the locations in reverse order.
     */
    fun startPatrol() {
        if (robot != null) {
            var patrolPath = robot!!.getLocations()

            // Debug
            Log.d(TAG, "Going to: ")
            patrolPath.forEach { location: String ->
                Log.d(TAG, location)
            }

            if (patrolPath.size > 0) {
                goToPoint(patrolPath, patrolPath.size - 1)
            }
        }
    }

    /**
     * Instructs Temi to go to a specific location based on the
     * position in the index.
     *
     * Once it arrives at that location, automatically proceed
     * to the next location.
     */
    fun goToPoint(patrolPath: List<String>, patrolPathIndex: Int) {
        Log.d(TAG, "goToPoint: " + patrolPathIndex)
        robot!!.goToLocationByName(patrolPath[patrolPathIndex]) {
            // This function is called once the robot arrives at the next point.

            Log.d(TAG, "goToPoint: " + patrolPathIndex + " complete!")

            if (patrolPathIndex - 1 >= 0) {
                goToPoint(patrolPath, patrolPathIndex - 1);
            } else {
                robot!!.goToLocationByName("HOME BASE")
            }
        }
    }

    /** This function is called whenever the user denies / grants the
     * permission to use the camera.
     */
    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<String>,
            grantResults: IntArray
    ) {
        Log.d(TAG, "Camera permissions result: " + permissions)
        when (requestCode) {
            CAMERA_PERMISSION_REQUEST -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // If permission is granted, let our OpenCV camera
                    // know.
                    //
                    Log.d(TAG, "Camera permissions - granted")
                    mOpenCvCameraView!!.setCameraPermissionGranted()
                } else {
                    val message = "Camera permissions - not granted"
                    Log.e(TAG, message)
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                }
            }
            else -> {
                Log.e(TAG, "Camera permissions -Unexpected permission request")
            }
        }
    }


    /**
     * Called when the activity pauses
     */
    override fun onPause() {
        super.onPause()
        if (mOpenCvCameraView != null)
            mOpenCvCameraView!!.disableView()
    }


    /**
     * Called when the activity resumes.
     */
    override fun onResume() {
        super.onResume()
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization")
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback)
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!")
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        }
    }


    /**
     * Called when the activity is destroyed.
     */
    override fun onDestroy() {
        super.onDestroy()
        if (mOpenCvCameraView != null)
            mOpenCvCameraView!!.disableView()
    }


    /**
     * Called when the camera (connected via OpenCV) ]is started.
     */
    override fun onCameraViewStarted(width: Int, height: Int) {
        Log.d(TAG, "Camera started: " + width + "x" + height)
        imageRgba = Mat(height, width, CvType.CV_8UC4)
    }

    /**
     * Called when the camera (connected via OpenCV) is stopped.
     */
    override fun onCameraViewStopped() {
        imageRgba?.release()
    }

    /**
     * This function is triggered for every frame of video captured from
     * the camera.
     */
    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame): Mat {
        val storage = Firebase.storage("gs://sda-tracker-app.appspot.com")
        val storageRef = storage.reference.child("violations")
        val db = Firebase.firestore
        // Retrieve the frame in RGBA format.
        //
        imageRgba = inputFrame.rgba()

        if (robot!!.isTemiAvailable()) {
            // When running in Temi, the camera image is flipped (180-degrees),
            // but format is correct in RGB.
            imageRgba = Helpers.rotateImage180(imageRgba!!)
        } else {
            // When running on emulators, the camera is upright, but format is in BGR.
            Imgproc.cvtColor(imageRgba, imageRgba, Imgproc.COLOR_BGR2RGB, 4)
        }

        if (isAnalyzingFrame == false) {
            isAnalyzingFrame = true

            Log.d(TAG, "Analysing frame " + imageRgba!!.cols() + "x" + imageRgba!!.rows())

            // First, make sure that we convert the image frame to gray, since
            // the Haar / HoG detectors only work on gray-scale images.
            //
            //convertImageFrameToGray()

            //=========================================================================
            // User Google ML Kit to detect faces
            //=========================================================================

            MLKitFaceDetector.detectAsync(imageRgba!!) { faces ->
                faceDetections = faces
            }

            //=========================================================================
            // Use MobileNet SSD to detect people.
            //=========================================================================

            var objDetections = TFLiteMobileNetObjDetector.detect(imageRgba!!)

            // Draw bounding boxes for people
            //
            val detectedpeople = arrayListOf<Int>()
            for (d in objDetections) {
                if (d.categories[0].score >= 0.6 && d.categories[0].label == "person") {
                    Log.d(TAG, d.toString())
                    Imgproc.putText(imageRgba, d.categories[0].label,
                            Point(d.boundingBox.left.toDouble() - 20, d.boundingBox.top.toDouble()), 1, 1.5, objRectColor, 2)
                    Imgproc.rectangle(imageRgba,
                            Point(d.boundingBox.left.toDouble(), d.boundingBox.top.toDouble()),
                            Point(d.boundingBox.right.toDouble(), d.boundingBox.bottom.toDouble()),
                            objRectColor, 3)
                    detectedpeople.add(objDetections.indexOf(d))
                }
            }


            // Draw bounding boxes for faces
            if (faceDetections != null) {
                val currentFaceDetections = faceDetections

                var notWearingMask = false
                for (f in currentFaceDetections!!) {
                    var croppedFace = Helpers.cropImage(imageRgba!!, f.boundingBox.left, f.boundingBox.top, f.boundingBox.right, f.boundingBox.bottom)

                    var maskProbability = FaceMaskDetector.detect(croppedFace)
                    if (maskProbability < 0.5) {
                        notWearingMask = true
                    }

                    Imgproc.putText(
                            imageRgba,
                            "Face (" + (if (!notWearingMask) "Mask" else "No mask"),
                            Point(f.boundingBox.left.toDouble() - 20, f.boundingBox.top.toDouble()),
                            1,
                            1.5,
                            (if (!notWearingMask) faceMaskRectColor else faceNoMaskRectColor),
                            2
                    )
                    Imgproc.rectangle(
                            imageRgba,
                            Point(f.boundingBox.left.toDouble(), f.boundingBox.top.toDouble()),
                            Point(f.boundingBox.right.toDouble(), f.boundingBox.bottom.toDouble()),
                            (if (!notWearingMask) faceMaskRectColor else faceNoMaskRectColor),
                            3
                    )
                }


                if (notWearingMask) {
                    faceWithNoMaskFrameCounter++

                    if (faceWithNoMaskFrameCounter >= 8) {
                        if (!robot!!.temiIsSpeaking) {
                            robot!!.speak("Please remember to put on your mask!")
                            faceWithNoMaskFrameCounter = 0
                            var uuid = UUID.randomUUID()
                            saveImage(this, imageRgba!!, uuid.toString() + ".jpg")
                            var fs= this.filesDir.absolutePath + "/image.jpg"
                            var file = Uri.fromFile(File(this.filesDir.absolutePath + "/"+ uuid.toString() + ".jpg"))
                            val Ref = storageRef.child("${file.lastPathSegment}")
                            val uploadTask = Ref.putFile(file)

                            val urlTask = uploadTask.continueWithTask { task ->
                                if (!task.isSuccessful) {
                                    task.exception?.let {
                                        throw it
                                    }
                                }
                                Ref.downloadUrl
                            }.addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val downloadUri = task.result
                                    val details = hashMapOf(
                                            "Location" to "MakerSpace",
                                            "RobotID" to "1",
                                            "timeCreated" to Calendar.getInstance().time
                                    )
                                    val imageDetails = hashMapOf(
                                            "DateTaken" to Calendar.getInstance().time,
                                            "Description" to "d",
                                            "LocalURI" to "s",
                                            "RemoteURI" to downloadUri
                                    )

                                    db.collection("Violations").document(uuid.toString())
                                            .set(details)
                                            .addOnSuccessListener { Log.d(TAG, "DocumentSnapshot successfully written!") }
                                            .addOnFailureListener { e -> Log.d(TAG, "Error writing document", e) }
                                    // unable to create subcollections
                                    db.collection("Violations").document(uuid.toString()).collection("Photos").document(uuid.toString())
                                            .set(imageDetails)
                                            .addOnSuccessListener { Log.d(TAG, "DocumentSnapshot successfully written!") }
                                            .addOnFailureListener { e -> Log.d(TAG, "Error writing document", e) }
                                } else {
                                    // Handle failures
                                    // ...
                                }
                            }
                            // Register observers to listen for when the download is done or if it fails
                            uploadTask.addOnFailureListener {
                                // Handle unsuccessful uploads
                            }.addOnSuccessListener { taskSnapshot ->
                                // taskSnapshot.metadata contains file metadata such as size, content-type, etc.
                                // ...
                            }
                        }
                    }
                } else {
                    faceWithNoMaskFrameCounter = 0
                }

            }

            // If we detect a minimum of 4 people in the frame,
            // issue a gentle reminder to people to keep their distance
            //
            if (detectedpeople.size >= 1) {
                detectedPeopleFrameCounter++
                if (detectedPeopleFrameCounter >= 8) {
                    if (!robot!!.isSpeaking()) {
                        robot!!.speak("Please keep your social distance of 1 meters apart")
                    }
                    detectedPeopleFrameCounter = 0
                }
            } else {
                detectedPeopleFrameCounter = 0
            }

            isAnalyzingFrame = false
        }

        // Returns the updated video frame to be displayed in our JavaCamerView.
        //
        return imageRgba!!
    }

    private fun sendNotification(notification: JSONObject) {
        Log.e("TAG", "sendNotification")
        val jsonObjectRequest = object : JsonObjectRequest(FCM_API, notification,
                Response.Listener<JSONObject> { response ->
                    Log.i("TAG", "onResponse: $response")
                },
                Response.ErrorListener {
                    Toast.makeText(this@MainActivity, "Request error", Toast.LENGTH_LONG).show()
                    Log.i("TAG", "onErrorResponse: Didn't work")
                }) {

            override fun getHeaders(): Map<String, String> {
                val params = HashMap<String, String>()
                params["Authorization"] = serverKey
                params["Content-Type"] = contentType
                return params
            }
        }
        requestQueue.add(jsonObjectRequest)
    }


}

