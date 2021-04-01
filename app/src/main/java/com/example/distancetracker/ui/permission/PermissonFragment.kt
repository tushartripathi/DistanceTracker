package com.example.distancetracker.ui.permission

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.example.distancetracker.R
import com.example.distancetracker.util.Permission.hasLocationPermission
import com.example.distancetracker.util.Permission.requestLocationPermisson
import com.example.distancetracker.databinding.FragmentPermissonBinding
import com.example.distancetracker.util.Permission.requestBackgroundPermisson
import com.vmadalin.easypermissions.EasyPermissions
import com.vmadalin.easypermissions.dialogs.SettingsDialog


class PermissonFragment : Fragment() , EasyPermissions.PermissionCallbacks {
    private var _binding: FragmentPermissonBinding?= null
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentPermissonBinding.inflate(inflater, container, false)

        binding.continueBtnID.setOnClickListener {
            if(hasLocationPermission(requireContext()))
                findNavController().navigate(R.id.action_permissonFragment_to_mapsFragment)
            else
                requestLocationPermisson(this)
        }
        return binding.root;
    }

    //Easypermissin lib method
    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {
        if(EasyPermissions.somePermissionPermanentlyDenied(this,perms[0]))
            SettingsDialog.Builder(requireActivity()).build().show()
        else
            requestBackgroundPermisson(this)
    }
    //Easypermissin lib method
    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {
       // findNavController().navigate(R.id.action_p_ermissonFragmentto_mapsFragment)


    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode,permissions,grantResults,this)
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding=null
    }
}