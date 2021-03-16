package sg.edu.nyp.erobot.robothelpers

import android.app.Activity
import android.util.Log
import android.widget.Toast
/*
import com.robotemi.sdk.Robot
import com.robotemi.sdk.TtsRequest
import com.robotemi.sdk.listeners.OnGoToLocationStatusChangedListener
import com.robotemi.sdk.listeners.OnRobotReadyListener
import com.robotemi.sdk.navigation.listener.OnCurrentPositionChangedListener
import com.robotemi.sdk.navigation.model.Position
*/

class RobotInterface
{
    class RobotPosition(val x: Float, val y: Float, val rotateAngle: Float, val headTiltAngle: Int) {
    }

    val TAG = "RobotInterface"

    var currentActivity: Activity? = null

    var toast: Toast? = null
    /*
    var temiRobot: Robot? = null
    var temiRobotReady = false
    var temiPosition : Position? = null
    var temiOnReadyAction: (() -> Unit)? = null
    var temiOnArriveAction: ((String) -> Unit)? = null
    var temiOnSpeakingCompleteAction: (() -> Unit)? = null
    */
    var temiIsSpeaking = false

    fun isTemiAvailable() : Boolean
    {
        return false
    }

    fun setActivity(activity: Activity)
    {
        currentActivity = activity
    }

    fun initializeRobot(onReady: (() -> Unit)?)
    {
        //temiRobot = Robot.getInstance()
        //temiOnReadyAction = onReady
        activityStarted()
    }


    fun getLocations() : List<String>
    {
        //if (temiRobot != null && temiRobotReady) {
        //    return temiRobot!!.locations
        //}
        return emptyList()
    }


    fun getPosition() : RobotPosition
    {
        //if (temiRobot != null && temiPosition != null) {
        //    var p = temiPosition!!
        //    return RobotPosition(p.x, p.y, p.yaw, p.tiltAngle)
        //}
        return RobotPosition(-1.0f, -1.0f, -1.0f, -1)
    }

    fun speak(text: String, onComplete: (() -> Unit)? = null)
    {
        /*if (temiRobot != null && temiRobotReady) {
            temiIsSpeaking = true
            temiOnSpeakingCompleteAction = onComplete
            temiRobot!!.speak(TtsRequest.create(text, false))
            currentActivity!!.runOnUiThread {
                Toast.makeText(currentActivity, text, 2000).show()
            }
        }*/

        currentActivity!!.runOnUiThread {
            if (toast != null)
            {
                toast!!.cancel()
            }

            toast = Toast.makeText(currentActivity, text, Toast.LENGTH_SHORT)
            toast!!.show()
        }

    }

    fun getBatteryLevel() : Int {
        //if (temiRobot != null && temiRobot!!.batteryData != null) {
        //    return temiRobot!!.batteryData!!.level
        //}
        return -1
    }

    fun isSpeaking() : Boolean
    {
        //return temiIsSpeaking
        return false;
    }

    fun goToLocationByName(location: String, onArrive: ((String) -> Unit)? = null)
    {
        /*if (temiRobot != null && temiRobotReady)
        {
            temiOnArriveAction = onArrive
            temiRobot!!.goTo(location)
        }*/
    }

    fun goToPosition(x: Float, y: Float, rotateAngle: Float, onArrive: ((String) -> Unit)? = null)
    {
        /*if (temiRobot != null && temiRobotReady)
        {
            temiOnArriveAction = onArrive
            temiRobot!!.goToPosition(Position(x, y, rotateAngle, 0))
        }*/
    }


    fun activityStarted() {
        //temiRobot!!.addOnRobotReadyListener(this)
        //temiRobot!!.addOnGoToLocationStatusChangedListener(this)
        //temiRobot!!.addTtsListener(this)
        //temiRobot!!.addOnCurrentPositionChangedListener(this)
    }

    fun activityStopped() {
        //temiRobot!!.removeOnRobotReadyListener(this)
        //temiRobot!!.removeOnGoToLocationStatusChangedListener(this)
        //temiRobot!!.removeTtsListener(this)
        //temiRobot!!.removeOnCurrentPositionChangedListener(this)
    }

    fun saveCurrentLocation(name: String)
    {
        //if (temiRobot != null && temiRobotReady)
        //{
        //    temiRobot!!.saveLocation(name)
        //}
    }

    fun deleteLocation(name: String)
    {
        //if (temiRobot != null && temiRobotReady)
        //{
        //    temiRobot!!.deleteLocation(name)
        //}
    }

    fun deleteAllLocations()
    {
        //if (temiRobot != null && temiRobotReady)
        //{
        //   temiRobot!!.locations.forEach { name ->
        //        temiRobot!!.deleteLocation(name)
        //    }
        //}
    }
}