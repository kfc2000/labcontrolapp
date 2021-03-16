package sg.edu.nyp.erobot.helpers

import android.app.Activity
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity

/**
 * Simple and clean class to help run code (updating of UI, etc) in the main thread.
 */
object Threading
{
    fun runOnMain(activity: Activity, actionToRun: () -> Unit)
    {
        Handler(activity.mainLooper).post(Runnable {
            actionToRun();
        })
    }
}