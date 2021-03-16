package sg.edu.nyp.erobot.robothelpers

import android.app.Activity
import android.util.Log
import android.widget.Toast
import com.robotemi.sdk.Robot
import com.robotemi.sdk.TtsRequest
import com.robotemi.sdk.listeners.OnGoToLocationStatusChangedListener
import com.robotemi.sdk.listeners.OnRobotReadyListener
import com.robotemi.sdk.navigation.listener.OnCurrentPositionChangedListener
import com.robotemi.sdk.navigation.model.Position
import sg.edu.nyp.erobot.helpers.Threading

class RobotInterface : OnRobotReadyListener, OnGoToLocationStatusChangedListener, Robot.TtsListener, OnCurrentPositionChangedListener
{
    class RobotPosition(val x: Float, val y: Float, val rotateAngle: Float, val headTiltAngle: Int) {
    }

    val TAG = "RobotInterface"

    var toast: Toast? = null
    var currentActivity: Activity? = null
    var temiRobot: Robot? = null
    var temiRobotReady = false
    var temiPosition : Position? = null

    var temiOnReadyAction: (() -> Unit)? = null
    var temiOnArriveAction: ((String) -> Unit)? = null
    var temiOnSpeakingCompleteAction: (() -> Unit)? = null

    var temiIsSpeaking = false

    fun isTemiAvailable() : Boolean
    {
        return true
    }

    fun setActivity(activity: Activity)
    {
        currentActivity = activity
    }

    fun initializeRobot(onReady: (() -> Unit)?)
    {
        temiRobot = Robot.getInstance()
        temiOnReadyAction = onReady
        activityStarted()
    }


    fun getLocations() : List<String>
    {
        if (temiRobot != null && temiRobotReady) {
            return temiRobot!!.locations
        }
        return emptyList()
    }


    fun getPosition() : RobotPosition
    {
        if (temiRobot != null && temiPosition != null) {
            var p = temiPosition!!
            return RobotPosition(p.x, p.y, p.yaw, p.tiltAngle)
        }
        return RobotPosition(-1.0f, -1.0f, -1.0f, -1)
    }

    fun speak(text: String, onComplete: (() -> Unit)? = null)
    {
        Log.d(TAG, "Speaking... " + text)
        if (temiRobot != null && temiRobotReady) {
            temiIsSpeaking = true
            temiOnSpeakingCompleteAction = onComplete
            temiRobot!!.speak(TtsRequest.create(text, false))

            currentActivity!!.runOnUiThread {
                if (toast != null)
                {
                    toast!!.cancel()
                }

                toast = Toast.makeText(currentActivity, text, 2000)
                toast!!.show()
            }

        }
    }

    fun getBatteryLevel() : Int {
        if (temiRobot != null && temiRobot!!.batteryData != null) {
            return temiRobot!!.batteryData!!.level
        }
        return -1
    }

    fun isSpeaking() : Boolean
    {
        return temiIsSpeaking
    }

    fun goToLocationByName(location: String, onArrive: ((String) -> Unit)? = null)
    {
        if (temiRobot != null && temiRobotReady)
        {
            temiOnArriveAction = onArrive
            temiRobot!!.goTo(location)

        }
    }

    fun goToPosition(x: Float, y: Float, rotateAngle: Float, onArrive: ((String) -> Unit)? = null)
    {
        if (temiRobot != null && temiRobotReady)
        {
            temiOnArriveAction = onArrive
            temiRobot!!.goToPosition(Position(x, y, rotateAngle, 0))
        }
    }

    override fun onGoToLocationStatusChanged(location: String, status: String, descriptionId: Int, description: String) {
        when (status) {
            OnGoToLocationStatusChangedListener.START -> {

            }
            OnGoToLocationStatusChangedListener.CALCULATING -> {

            }
            OnGoToLocationStatusChangedListener.GOING -> {

            }
            OnGoToLocationStatusChangedListener.COMPLETE -> {
                Log.d(TAG, "OnGoToLocationStatusChangedListener.COMPLETE")
                if (temiOnArriveAction != null) {
                    val onArriveAction = temiOnArriveAction
                    temiOnArriveAction = null

                    onArriveAction!!(location)
                }
            }
            OnGoToLocationStatusChangedListener.ABORT -> {

            }
        }
    }

    override fun onRobotReady(isReady: Boolean) {
        temiRobotReady = isReady

        if (isReady)
        {
            temiOnReadyAction!!()
        }
    }


    fun activityStarted() {
        temiRobot!!.addOnRobotReadyListener(this)
        temiRobot!!.addOnGoToLocationStatusChangedListener(this)
        temiRobot!!.addTtsListener(this)
        temiRobot!!.addOnCurrentPositionChangedListener(this)
    }

    fun activityStopped() {
        temiRobot!!.removeOnRobotReadyListener(this)
        temiRobot!!.removeOnGoToLocationStatusChangedListener(this)
        temiRobot!!.removeTtsListener(this)
        temiRobot!!.removeOnCurrentPositionChangedListener(this)
    }

    override fun onTtsStatusChanged(ttsRequest: TtsRequest) {
        when (ttsRequest.status)
        {
            TtsRequest.Status.STARTED -> {

            }
            TtsRequest.Status.COMPLETED -> {
                temiIsSpeaking = false
                if (temiOnSpeakingCompleteAction != null) {
                    temiOnSpeakingCompleteAction!!()
                    temiOnSpeakingCompleteAction = null;
                }
            }

            else -> {

            }
        }
    }

    override fun onCurrentPositionChanged(position: Position) {
        Log.d(TAG, "Position: " + position.x + "," + position.y + "," + position.yaw + " | " + position.tiltAngle)
        temiPosition = position
    }


    fun saveCurrentLocation(name: String)
    {
        if (temiRobot != null && temiRobotReady)
        {
            temiRobot!!.saveLocation(name)
        }
    }

    fun deleteLocation(name: String)
    {
        if (temiRobot != null && temiRobotReady)
        {
            temiRobot!!.deleteLocation(name)
        }
    }

    fun deleteAllLocations()
    {
        if (temiRobot != null && temiRobotReady)
        {
            temiRobot!!.locations.forEach { name ->
                temiRobot!!.deleteLocation(name)
            }
        }
    }
}
