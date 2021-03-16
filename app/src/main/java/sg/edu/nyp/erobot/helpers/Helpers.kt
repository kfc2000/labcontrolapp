package sg.edu.nyp.erobot.helpers

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.util.Log
import android.widget.ImageView
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

object Helpers {
    var TAG = "Helpers"

    /**
     * Copies a file from the res/raw folder into the files folder
     * where it is accessible by your application through its file name.
     */
    fun copyFileFromRawFolderToFilesFolder(activity: Activity, id: Int, filename: String)
    {
        var destFileName = File(activity.filesDir.absolutePath + "/" + filename)

        if (!destFileName.exists()) {
            val inStream: InputStream
            var outStream: FileOutputStream
            try {
                inStream = activity.resources.openRawResource(id)

                outStream = FileOutputStream(destFileName)
                val buffer = ByteArray(4096)
                var bytesRead: Int = 0
                while (inStream.read(buffer).also({ bytesRead = it }) != -1) {
                    outStream.write(buffer, 0, bytesRead)
                }
                inStream.close()
                outStream.close()
            } catch (e: IOException) {
                Log.i(TAG, "Error copying file")
            }
        }
    }


    /**
     * Saves an OpenCV image (in Mat class) to a JPG/PNG in the files folder.
     */
    fun saveImage(activity: Activity, mat: Mat, filename: String) {
        val imgFile = File(activity.filesDir.absolutePath + "/" + filename)

        //Log.d(TAG, "Saving to... " + imgFile.absolutePath)
        Imgcodecs.imwrite(imgFile.absolutePath, mat)
    }

    /**
     * Loads an image from a file and returns it as a bitmap.
     */
    fun loadImage(activity: Activity, filename: String) : Bitmap? {
        val imgFile = File(activity.filesDir.absolutePath + "/" + filename)

        if(imgFile.exists())
        {
            //BitmapFactory.dec
            return BitmapFactory.decodeFile(imgFile.absolutePath)
        }
        return null
    }


    /**
     * Loads an image from a file and displays it in an ImageView
     */
    fun loadAndDisplayImage(activity: Activity, filename: String, imageView: ImageView) {
        val imgFile = File(activity.filesDir.absolutePath + "/" + filename)

        if(imgFile.exists())
        {
            Handler(activity.baseContext.mainLooper).post(Runnable {
                // This runs in the main thread.
                val myBitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
                imageView.setImageBitmap(myBitmap)
            })
        }
    }



    /**
     * Converts an RGB image matrix into grayscale
     */
    fun toGrayscale(mat: Mat) : Mat
    {
        var newMat = Mat()
        Imgproc.cvtColor(mat, newMat, Imgproc.COLOR_RGB2GRAY, 4)
        return newMat
    }

    /**
     * Crops an image from (x1, y1) to (x2, y2)
     */
    fun cropImage(mat: Mat, x1: Int, y1: Int, x2: Int, y2: Int) : Mat
    {
        var ax1 = x1
        var ay1 = y1
        var width = x2 - x1
        var height = y2 - y1

        if (ax1 < 0)
        {
            width = width - ax1
            ax1 = 0
        }
        if (ay1 < 0)
        {
            height = height - ay1
            ay1 = 0
        }
        if (ax1 + width > mat.cols())
            width = mat.cols() - ax1 - 1
        if (ay1 + height > mat.rows())
            height = mat.rows() - ay1 - 1


        Log.d("Helpers", "Crop: " + ax1 + "," + ay1 + " / " + width + "x" + height + " orig:" + mat.cols() + "x" + mat.rows())
        return Mat(mat, Rect(ax1, ay1, width, height))
    }


    /**
     * Resizes an image to the target width / height.
     */
    fun resizeImage(mat: Mat, width: Int, height: Int) : Mat
    {
        val newMat = Mat.zeros(width, height, CvType.CV_8UC4)
        Imgproc.resize(mat, newMat, Size(width.toDouble(), height.toDouble()),0.0, 0.0, Imgproc.INTER_CUBIC)
        return newMat
    }

    /**
     * Crops a bitmap from (x1, y1) to (x2, y2)
     */
    fun cropImage(bitmap: Bitmap, x1: Int, y1: Int, x2: Int, y2: Int) : Bitmap
    {
        val oldMat = Mat.zeros(bitmap.width, bitmap.height, CvType.CV_8UC4)
        Utils.bitmapToMat(bitmap, oldMat);

        val croppedMat = cropImage(oldMat, x1, y1, x2, y2)
        return matToBitmap(croppedMat)
    }


    /**
     * Rotates an image matrix 180 degrees.
     */
    fun rotateImage180(mat: Mat): Mat
    {
        val newMat = Mat()
        Core.rotate(mat, newMat, Core.ROTATE_180)
        return newMat
    }


    /**
     * Rotates an image matrix 90 degrees clockwise.
     */
    fun rotateImage90Clockwise(mat: Mat): Mat
    {
        val newMat = Mat()
        Core.rotate(mat, newMat, Core.ROTATE_90_CLOCKWISE)
        return newMat
    }

    /**
     * Rotates an image matrix 90 degrees counter-clockwise.
     */
    fun rotateImage90CounterClockwise(mat: Mat): Mat
    {
        val newMat = Mat()
        Core.rotate(mat, newMat, Core.ROTATE_90_COUNTERCLOCKWISE)
        return newMat
    }


    /**
     * Converts an OpenCV Mat into a Bitmap.
     */
    fun matToBitmap(mat: Mat) : Bitmap
    {
        val newBitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(mat, newBitmap)
        return newBitmap
    }


    /**
     * Converts an OpenCV RGBA matrix to RGB FloatArray
     */
    fun matToRGBFloatArray(mat: Mat, factorToMultiply: Double = 1.0, valueToAdd: Double = 0.0) : FloatArray
    {
        //Log.d("Helpers", "matToFloatArray: " + mat.type() + " " + mat.cols() + "x" + mat.rows() + "x" + mat.channels())


        var newMat = Mat()

        mat.convertTo(newMat, CvType.CV_32FC3, factorToMultiply, valueToAdd)
        val arr = FloatArray((mat.total() * 4).toInt())
        newMat.get(0, 0, arr)

        //
        var arrNoAlpha = FloatArray((mat.total() * 3).toInt())
        for (i in 0 .. mat.total().toInt() - 1)
        {
            arrNoAlpha[i*3 + 0] = arr[i*4 + 0]
            arrNoAlpha[i*3 + 1] = arr[i*4 + 1]
            arrNoAlpha[i*3 + 2] = arr[i*4 + 2]
        }

        return arrNoAlpha
    }

}