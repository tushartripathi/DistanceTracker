package com.example.distancetracker.util

import android.hardware.Camera
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import java.text.DecimalFormat

object MapUtils {

    fun setCameraPosition(location: LatLng) : CameraPosition{
        return CameraPosition.Builder()
                .target(location)
                .zoom(16f)
                .build()
    }

    fun calculteElapsedTime(start:Long, stop:Long) : String
    {
        val totalTime = stop-start
        val seconds = (totalTime/1000).toInt() % 60
        val minutes = (totalTime/(1000*60)) % 60
        val hours = (totalTime/ (1000*60*60) % 60)

        return "$hours:$minutes:$seconds"
    }

    fun calculateDistance(locationList:MutableList<LatLng>):String{
        if(locationList.isNotEmpty())
        {
            val meters = SphericalUtil.computeDistanceBetween(locationList[0],locationList.last())
            val km = meters/1000
            return DecimalFormat("#.##").format(km)
        }
        return "0.00"
    }
}