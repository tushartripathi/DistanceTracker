package com.example.distancetracker

import android.annotation.SuppressLint
import androidx.fragment.app.Fragment

import android.os.Bundle
import android.os.CountDownTimer
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.distancetracker.databinding.FragmentMapsBinding
import com.example.distancetracker.util.ExtensionFunctions.disable
import com.example.distancetracker.util.ExtensionFunctions.enable
import com.example.distancetracker.util.ExtensionFunctions.hide
import com.example.distancetracker.util.ExtensionFunctions.show
import com.example.distancetracker.util.Permission
import com.example.distancetracker.util.Permission.hasBackgroundPermission
import com.example.distancetracker.util.Permission.requestBackgroundPermisson

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.vmadalin.easypermissions.EasyPermissions
import com.vmadalin.easypermissions.dialogs.SettingsDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.zip.CheckedOutputStream

class MapsFragment : Fragment() , OnMapReadyCallback, GoogleMap.OnMyLocationButtonClickListener, EasyPermissions.PermissionCallbacks {

    private var  _binding: FragmentMapsBinding?=null
    private val bindng get() = _binding!!
    private lateinit var map:GoogleMap

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
        bindng.stopBtnID.setOnClickListener {  }
        bindng.resetBtnID.setOnClickListener {  }

        return bindng.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)
    }



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


    }

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



    override fun onMyLocationButtonClick(): Boolean {
       bindng.tapLocationTextID.animate().alpha(0f).duration=1500
        lifecycleScope.launch {
            delay(1500)
            bindng.tapLocationTextID.hide()
            bindng.startBtnID.show()
        }

        return false

    }

    //to avoid memory leaks
    override fun onDestroyView() {
        super.onDestroyView()
        _binding=null
    }

    fun onStartButtonClicked()
    {
        if(hasBackgroundPermission(requireContext()))
        {

        }
        else
        {
            requestBackgroundPermisson(this)
        }
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
                if(currentSceond.toString()=="0")
                {
                    bindng.countDownTextID.text="GO"
                    bindng.countDownTextID.setTextColor(ContextCompat.getColor(requireContext(),R.color.green_700))
                }
                else
                {
                    bindng.countDownTextID.text=currentSceond.toString()
                }
            }

            override fun onFinish() {
                bindng.stopBtnID.enable()
                bindng.countDownTextID.hide()

            }

        }
        timer.start()


    }
}