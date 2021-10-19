package com.daerong.graduationproject.beacon

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.daerong.graduationproject.R
import com.daerong.graduationproject.databinding.FragmentSelectLocBinding


class SelectLocFragment : Fragment() {
    lateinit var binding: FragmentSelectLocBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSelectLocBinding.inflate(inflater, container, false)
        return binding.root
    }

}