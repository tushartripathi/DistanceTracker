package com.example.distancetracker.ui.maps

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import androidx.fragment.app.Fragment

import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.util.lruCache
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.distancetracker.R
import com.example.distancetracker.databinding.FragmentMapsBinding
import com.example.distancetracker.model.Result
import com.example.distancetracker.service.TrackerService
import com.example.distancetracker.service.TrackerService.Companion.startTime
import com.example.distancetracker.ui.MainActivity
import com.example.distancetracker.util.Constants.ACTION_SERVICE_START
import com.example.distancetracker.util.Constants.ACTION_SERVICE_STOP
import com.example.distancetracker.util.ExtensionFunctions.disable
import com.example.distancetracker.util.ExtensionFunctions.enable
import com.example.distancetracker.util.ExtensionFunctions.hide
import com.example.distancetracker.util.ExtensionFunctions.show
import com.example.distancetracker.util.MapUtils
import com.example.distancetracker.util.MapUtils.calculateDistance
import com.example.distancetracker.util.MapUtils.calculteElapsedTime
import com.example.distancetracker.util.MapUtils.setCameraPosition
import com.example.distancetracker.util.Permission
import com.example.distancetracker.util.Permission.hasBackgroundPermission
import com.example.distancetracker.util.Permission.requestBackgroundPermisson
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory

import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.vmadalin.easypermissions.EasyPermissions
import com.vmadalin.easypermissions.dialogs.SettingsDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class MapsFragment : Fragment() ,
        OnMapReadyCallback,
        GoogleMap.OnMyLocationButtonClickListener,
        EasyPermissions.PermissionCallbacks,
        GoogleMap.OnMarkerClickListener{

    private var  _binding: FragmentMapsBinding?=null            // Data Binding variable
    private val bindng get() = _binding!!                      // data binding getter fn() call
    private lateinit var map:GoogleMap                          // map inside fragment
    private var started = MutableLiveData(false)           // boolean value to check tracking is started
    private var startTime = 0L                                  //store starting time
    private var stopTime = 0L                                   //store last time
    private var locationList = mutableListOf<LatLng>()              //List of latng obj values of all the location travelled
    private var polyLineList = mutableListOf<Polyline>()          // To draw the line on the map
    private var markerList = mutableListOf<Marker>()               // Place marker at starting and end point
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient          // to get last location

    /////////////////////////////////////////////// Fragment Lifecycle Methods ///////////////////////////////////////

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        //my code
        _binding = FragmentMapsBinding.inflate(inflater, container, false) //data binding method
        fusedLocationProviderClient = LocationServices.
                                    getFusedLocationProviderClient(
                                            requireActivity())              // initlizing fused location

        bindng.startBtnID.setOnClickListener {
            onStartButtonClicked()                                          //Starting the process
            startCountDown()                                                //start countdown
        }
        bindng.stopBtnID.setOnClickListener { onStopButtonClicked()
        }
        bindng.resetBtnID.setOnClickListener { restMap() }


        return bindng.root
    }



    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding=null  //to avoid memory leaks
    }

    /////////////////////////////////////////////// Fragment Lifecycle Methods End ///////////////////////////////////////

/////////////////////////////////////////// Map inbuit methods////////////////////////////////////////////////
    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap?) {

        map = googleMap!!                                         // assing global variable map
        map.isMyLocationEnabled=true                              // current location button visible
        map.setOnMyLocationButtonClickListener(this)              // current location button click listner initlize
        map.setOnMarkerClickListener(this)                        // marker click listener initlized

        //Visible map designing
        map.uiSettings.apply {
            isZoomControlsEnabled=false
            isZoomGesturesEnabled = false
            isRotateGesturesEnabled=false
            isTiltGesturesEnabled=false
            isScrollGesturesEnabled=false
        }

         //most important
         observeTrackerService()                                 // Keep track of the backgroud service
    }
    /////////////////////////////// Map on Click Listener //////////////////////////////////////////////////////
    // current location click listner funcstion
    override fun onMyLocationButtonClick(): Boolean {

        bindng.tapLocationTextID.animate().alpha(0f).duration=1500     // create fading animcation

        lifecycleScope.launch {
            delay(1500)                 // delay for better UX
            bindng.tapLocationTextID.hide()      // hide textview
            bindng.startBtnID.show()             // show start button
        }
        return false
    }
    /////////////////////////////// Map on Click Listener End //////////////////////////////////////////////////////
    /////////////////////////////////////////// Map inbuit methods Ends////////////////////////////////////////////////

    /////////////////////////////// Permissions //////////////////////////////////////////////////////
    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {
        if(EasyPermissions.somePermissionPermanentlyDenied(this,perms[0]))
            SettingsDialog.Builder(requireActivity()).build().show()
        else
            Permission.requestLocationPermisson(this)
    }

    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode,permissions,grantResults,this)
    }

    /////////////////////////////// Permissions Ending //////////////////////////////////////////////////////




    private fun observeTrackerService()
    {
        //Tracker service is a Backgroud and foreground service
        // Observing Livedata vales in Service
        
        TrackerService.locationList.observe(viewLifecycleOwner,{
            if(it!=null)
            {
                locationList= it
                Log.d("LocationList",locationList.toString())
               // Toast.makeText(activity ,""+ locationList, Toast.LENGTH_SHORT).show()
                drawPolyline()// draw line on the map
                focusPolyLine()//change camera focus to latest location

            }
        })

        TrackerService.startTime.observe(viewLifecycleOwner,{
            startTime= it
        })

        TrackerService.stopTime.observe(viewLifecycleOwner,{
            stopTime=it
            if(stopTime!=0L)
            {
                showBiggerPicture()
                displayResults()
            }
        })

        TrackerService.started.observe(viewLifecycleOwner,{
            started.value=it
            Toast.makeText(activity,"Wor", Toast.LENGTH_SHORT).show()
            if(started.value!!)
            {
                Toast.makeText(activity,"Working ..", Toast.LENGTH_SHORT).show()
                bindng.tapLocationTextID.hide()
                bindng.stopBtnID.show()
                bindng.startBtnID.hide()
            }
            else
            {
                bindng.stopBtnID.hide()
                bindng.startBtnID.show()
                bindng.startBtnID.disable()
                Toast.makeText(activity,"not ..", Toast.LENGTH_SHORT).show()
            }

        })


    }

    fun onStopButtonClicked()
    {
        stopForegroundService()
        bindng.stopBtnID.hide()
        bindng.startBtnID.show()
        bindng.startBtnID.disable()
    }

    private fun stopForegroundService() {
      sendActionCommandToService(ACTION_SERVICE_STOP)
    }

    fun onStartButtonClicked()
    {
        if(hasBackgroundPermission(requireContext())) { // checking for permissions
        }
        else {
            requestBackgroundPermisson(this)
        }
    }
    private fun focusPolyLine()
    {
        if(locationList.isNotEmpty())
        {
            map.animateCamera(
                    CameraUpdateFactory.newCameraPosition(
                            setCameraPosition(locationList.last())),1000,null)
        }
    }


    private fun drawPolyline()
    {
        val polyline = map.addPolyline(
                PolylineOptions().apply {
                    width(10f)
                    color(Color.BLUE)
                    jointType(JointType.ROUND)
                    startCap(ButtCap())
                    endCap(ButtCap())
                    addAll(locationList)
                }
        )

        polyLineList.add(polyline)
    }

    fun startCountDown()
    {
        bindng.startBtnID.hide()
        bindng.stopBtnID.show()
        bindng.stopBtnID.disable()
        bindng.countDownTextID.show()
        val timer : CountDownTimer = object :CountDownTimer(4000,1000){
            override fun onTick(millisUntilFinished: Long) {
                val currentSceond = millisUntilFinished/1000
                if(currentSceond.toString()=="0") {
                    bindng.countDownTextID.text="GO"
                    bindng.countDownTextID.setTextColor(ContextCompat.getColor(requireContext(), R.color.green_700))
                }
                else {
                    bindng.countDownTextID.text=currentSceond.toString()
                }
            }
            override fun onFinish() {
                bindng.stopBtnID.enable()
                bindng.countDownTextID.hide()
                sendActionCommandToService(ACTION_SERVICE_START)
            }
        }
        timer.start()
    }

    private fun sendActionCommandToService(action:String) {
        Intent(requireContext(),
        TrackerService::class.java).apply {
            this.action = action
            requireContext().startService(this)
        }
    }


    private fun showBiggerPicture() {

        val bounds = LatLngBounds.Builder()
        for(location in locationList)
        {
            bounds.include(location)
        }
        map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 100),2000,null)
        addMarker(locationList.first())
        addMarker(locationList.last())
    }

    private fun addMarker(position :LatLng)
    {
            val marker = map.addMarker(MarkerOptions().position(position))
            markerList.add(marker)
    }

    private fun displayResults()
    {
        val result = Result(calculateDistance(locationList), calculteElapsedTime(startTime,stopTime))

        lifecycleScope.launch {
            delay(2500)
            val directions = MapsFragmentDirections.actionMapsFragmentToResultFragment(result)
            findNavController().navigate(directions)
            bindng.startBtnID.apply {
                hide()
                enable()
            }
            bindng.startBtnID.hide()
            bindng.resetBtnID.show()
        }
    }

    @SuppressLint("MissingPermission")
    private fun restMap() {

            fusedLocationProviderClient.lastLocation.addOnCompleteListener{
                val lastLocation = LatLng(
                        it.result.latitude,
                        it.result.longitude
                )

                map.animateCamera(CameraUpdateFactory.newCameraPosition(setCameraPosition(lastLocation)))
            }

        for(polyline in polyLineList) {
            polyline.remove()
        }
        locationList.clear()
        for(marker in markerList)
        {
            marker.remove()
        }
        markerList.clear()
        bindng.resetBtnID.hide()
        bindng.startBtnID.show()
    }

    override fun onMarkerClick(p0: Marker?): Boolean {

        return true
    }


}