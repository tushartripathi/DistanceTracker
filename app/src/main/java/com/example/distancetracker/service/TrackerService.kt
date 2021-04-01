package com.example.distancetracker.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_LOW
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import com.example.distancetracker.util.Constants.ACTION_SERVICE_START
import com.example.distancetracker.util.Constants.ACTION_SERVICE_STOP
import com.example.distancetracker.util.Constants.LOCATION_FASTEST_UPDATE_INTERVAL
import com.example.distancetracker.util.Constants.LOCATION_UPDATE_INTERVAL
import com.example.distancetracker.util.Constants.NOTIFICATION_CHANNEL_ID
import com.example.distancetracker.util.Constants.NOTIFICATION_CHHANNEL_NAME
import com.example.distancetracker.util.Constants.NOTIFICATTION_ID
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TrackerService : LifecycleService() {

    @Inject
    lateinit var notification: NotificationCompat.Builder
    @Inject
    lateinit var notificationManager: NotificationManager
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    companion object{
        val started = MutableLiveData<Boolean>()
        val locationList = MutableLiveData<MutableList<LatLng>>()
        val startTime = MutableLiveData<Long>()
        val stopTime = MutableLiveData<Long>()

    }
    private val locationCallback = object: LocationCallback()
    {
        override fun onLocationResult(result: LocationResult?) {
            super.onLocationResult(result)

            result?.locations?.let { locations ->
                for(location in locations)
                {
                    Log.d("test","1")
                    updatLocationList(location)
                }
            }
        }
    }
    private fun updatLocationList(locattion: Location)
    {
    val newLatLong = LatLng(locattion.latitude, locattion.longitude)
        locationList.value?.apply {
            add(newLatLong)
            locationList.postValue(this)
        }
    }

    private fun setInitialValues()
    {
        started.postValue(false)
        locationList.postValue(mutableListOf())
        startTime.postValue(0)
        stopTime.postValue(0)
    }

    override fun onCreate() {
        super.onCreate()
        setInitialValues()
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when(it.action) {
                ACTION_SERVICE_START->{
                    started.postValue(true)
                    startForeGroundService()
                    startLocationUpdate()
                }
                ACTION_SERVICE_STOP-> {
                    started.postValue(false)
                    stopForegroundService()
                }
                else ->{}
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }



    @SuppressLint("MissingPermission")
    private fun startLocationUpdate()
    {
        val locationRequest = LocationRequest().apply {
            interval= LOCATION_UPDATE_INTERVAL
            fastestInterval = LOCATION_FASTEST_UPDATE_INTERVAL
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        fusedLocationProviderClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper())

        startTime.postValue(System.currentTimeMillis())
    }

    private fun stopForegroundService() {
       removeLocationUpdates()
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(NOTIFICATTION_ID) // close notification
        stopForeground(true)
        stopSelf()
        stopTime.postValue(System.currentTimeMillis())
    }

    private fun removeLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)

    }

    private fun createNotificationChannel()
    {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHHANNEL_NAME, IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun startForeGroundService()
    {
        createNotificationChannel()
        startForeground(NOTIFICATTION_ID,notification.build())
    }
}