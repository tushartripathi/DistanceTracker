package com.example.distancetracker.ui.result

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import com.example.distancetracker.R
import com.example.distancetracker.databinding.FragmentResultBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment


class ResultFragment : BottomSheetDialogFragment() {

    private val args :ResultFragmentArgs by navArgs()
    private var _binding : FragmentResultBinding?=null
    private  val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View
    {
        _binding = FragmentResultBinding.inflate(inflater, container, false)
        binding.distanceValueID.text = getString(R.string.result, args.result.distance)
        binding.timeValueID.text = args.result.time
        binding.shareButtonID.setOnClickListener {
            shareResult()
        }
        return binding.root
    }

    private fun shareResult() {
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "I went ${args.result.distance} km in ${args.result.time}! \n show me what you got...")
        }

        startActivity(shareIntent)
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}