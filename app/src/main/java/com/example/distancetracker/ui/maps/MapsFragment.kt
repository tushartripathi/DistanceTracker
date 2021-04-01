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
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.example.distancetracker.R
import com.example.distancetracker.databinding.FragmentMapsBinding
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
import com.example.distancetracker.util.MapUtils.setCameraPosition
import com.example.distancetracker.util.Permission
import com.example.distancetracker.util.Permission.hasBackgroundPermission
import com.example.distancetracker.util.Permission.requestBackgroundPermisson
import com.google.android.gms.maps.CameraUpdateFactory

import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.vmadalin.easypermissions.EasyPermissions
import com.vmadalin.easypermissions.dialogs.SettingsDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class MapsFragment : Fragment() , OnMapReadyCallback, GoogleMap.OnMyLocationButtonClickListener, EasyPermissions.PermissionCallbacks {

    private var  _binding: FragmentMapsBinding?=null
    private val bindng get() = _binding!!
    private lateinit var map:GoogleMap
    private var started = MutableLiveData(false)
    private var startTime = 0L
    private var stopTime = 0L
    private var locationList = mutableListOf<LatLng>()

    /////////////////////////////////////////////// Fragment Lifecycle Methods ///////////////////////////////////////

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMapsBinding.inflate(inflater, container, false)

        bindng.startBtnID.setOnClickListener {
            onStartButtonClicked()
            startCountDown()
        }
        bindng.stopBtnID.setOnClickListener {
            onStopButtonClicked()
        }
        bindng.resetBtnID.setOnClickListener {  }
        return bindng.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)
    }
    //to avoid memory leaks
    override fun onDestroyView() {
        super.onDestroyView()
        _binding=null
    }

    /////////////////////////////////////////////// Fragment Lifecycle Methods End ///////////////////////////////////////

/////////////////////////////////////////// Map inbuit methods////////////////////////////////////////////////
    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap?) {

        map = googleMap!!
        map.isMyLocationEnabled=true
        map.setOnMyLocationButtonClickListener(this)
        map.uiSettings.apply {
            isZoomControlsEnabled=false
            isZoomGesturesEnabled = false
            isRotateGesturesEnabled=false
            isTiltGesturesEnabled=false
            isScrollGesturesEnabled=false
        }
         observeTrackerService()
    }
    /////////////////////////////// Map on Click Listener //////////////////////////////////////////////////////
    override fun onMyLocationButtonClick(): Boolean {
        bindng.tapLocationTextID.animate().alpha(0f).duration=1500
        lifecycleScope.launch {
            delay(1500)
            bindng.tapLocationTextID.hide()
            bindng.startBtnID.show()
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
        if(hasBackgroundPermission(requireContext())) {
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
    }


}