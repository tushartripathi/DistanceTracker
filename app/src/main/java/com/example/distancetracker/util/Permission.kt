package com.example.distancetracker.util

import android.content.Context
import com.vmadalin.easypermissions.EasyPermissions
import android.Manifest
import android.os.Build
import androidx.fragment.app.Fragment
import com.example.distancetracker.util.Constants.PERMISSION_BACKGROUND_REQUEST_CODE
import com.example.distancetracker.util.Constants.PERMISSON_LOCATION_REQUEST_CODE

object Permission
{
    fun hasLocationPermission(context: Context) =
        EasyPermissions.hasPermissions(
            context,Manifest.permission.ACCESS_FINE_LOCATION
        )


    fun requestLocationPermisson(fragment: Fragment)    {
        EasyPermissions.requestPermissions(
            fragment,
            "This application cannot work without Location Permission",
            PERMISSON_LOCATION_REQUEST_CODE,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    fun hasBackgroundPermission(context: Context):Boolean {             //Check if permission is granted

        if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.Q)                     // checking version
        {
            return EasyPermissions.hasPermissions(
                    context, Manifest.permission.ACCESS_BACKGROUND_LOCATION)  //using EasyPermission Lib for permission
        }
        else
            return true
    }

    //When permission is not granted
    fun requestBackgroundPermisson(fragment: Fragment)    {         // create permission request
        if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.Q) {
            EasyPermissions.requestPermissions(
                    fragment,
                    "This application cannot work without Location Permission",
                    PERMISSION_BACKGROUND_REQUEST_CODE,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
    }
}