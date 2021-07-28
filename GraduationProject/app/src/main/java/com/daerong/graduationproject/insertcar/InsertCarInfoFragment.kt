package com.daerong.graduationproject.insertcar

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.daerong.graduationproject.R
import com.daerong.graduationproject.databinding.FragmentInsertCarInfoBinding


class InsertCarInfoFragment : Fragment() {

    private var binding : FragmentInsertCarInfoBinding? = null
    private val insertCarViewModel : InsertCarViewModel by activityViewModels<InsertCarViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentInsertCarInfoBinding.inflate(layoutInflater,container,false)
        return binding?.root
    }


    override fun onDestroy() {
        super.onDestroy()
    }
}